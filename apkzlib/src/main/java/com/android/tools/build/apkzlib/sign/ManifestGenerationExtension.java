/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.build.apkzlib.sign;

import com.android.tools.build.apkzlib.utils.CachedSupplier;
import com.android.tools.build.apkzlib.utils.IOExceptionRunnable;
import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.android.tools.build.apkzlib.zfile.ManifestAttributes;
import com.android.tools.build.apkzlib.zip.StoredEntry;
import com.android.tools.build.apkzlib.zip.ZFile;
import com.android.tools.build.apkzlib.zip.ZFileExtension;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.Nullable;

public class ManifestGenerationExtension {

  private static final String META_INF_DIR = "META-INF";

  static final String MANIFEST_NAME = META_INF_DIR + "/MANIFEST.MF";

  private final String builtBy;

  private final String createdBy;

  @Nullable private ZFile zFile;

  private final Manifest manifest;

  private final CachedSupplier<byte[]> manifestBytes;

  private boolean dirty;

  @Nullable private ZFileExtension extension;

  public ManifestGenerationExtension(String builtBy, String createdBy) {
    this.builtBy = builtBy;
    this.createdBy = createdBy;
    manifest = new Manifest();
    dirty = false;
    manifestBytes =
        new CachedSupplier<>(
            () -> {
              ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
              try {
                manifest.write(outBytes);
              } catch (IOException e) {
                throw new IOExceptionWrapper(e);
              }

              return outBytes.toByteArray();
            });
  }

  private void markDirty() {
    dirty = true;
    manifestBytes.reset();
  }

  public void register(ZFile zFile) throws IOException {
    Preconditions.checkState(extension == null, "register() has already been invoked.");
    this.zFile = zFile;

    rebuildManifest();

    extension =
        new ZFileExtension() {
          @Nullable
          @Override
          public IOExceptionRunnable beforeUpdate() {
            return ManifestGenerationExtension.this::updateManifest;
          }
        };

    this.zFile.addZFileExtension(extension);
  }

  private void rebuildManifest() throws IOException {
    Verify.verifyNotNull(zFile, "zFile == null");

    StoredEntry manifestEntry = zFile.get(MANIFEST_NAME);

    if (manifestEntry != null) {
      /*
       * Read the manifest entry in the zip file. Make sure we store these byte sequence
       * because writing the manifest may not generate the same byte sequence, which may
       * trigger an unnecessary re-sign of the jar.
       */
      manifest.clear();
      byte[] manifestBytes = manifestEntry.read();
      manifest.read(new ByteArrayInputStream(manifestBytes));
      this.manifestBytes.precomputed(manifestBytes);
    }

    Attributes mainAttributes = manifest.getMainAttributes();
    String currentVersion = mainAttributes.getValue(ManifestAttributes.MANIFEST_VERSION);
    if (currentVersion == null) {
      setMainAttribute(
          ManifestAttributes.MANIFEST_VERSION, ManifestAttributes.CURRENT_MANIFEST_VERSION);
    } else {
      if (!currentVersion.equals(ManifestAttributes.CURRENT_MANIFEST_VERSION)) {
        throw new IOException("Unsupported manifest version: " + currentVersion + ".");
      }
    }

    /*
     * We "blindly" override all other main attributes.
     */
    setMainAttribute(ManifestAttributes.BUILT_BY, builtBy);
    setMainAttribute(ManifestAttributes.CREATED_BY, createdBy);
  }

  private void setMainAttribute(String attribute, String value) {
    Attributes mainAttributes = manifest.getMainAttributes();
    String current = mainAttributes.getValue(attribute);
    if (!value.equals(current)) {
      mainAttributes.putValue(attribute, value);
      markDirty();
    }
  }

  private void updateManifest() throws IOException {
    Verify.verifyNotNull(zFile, "zFile == null");

    if (!dirty) {
      return;
    }

    zFile.add(MANIFEST_NAME, new ByteArrayInputStream(manifestBytes.get()));
    dirty = false;
  }
}