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

package com.android.tools.build.apkzlib.zip;

import com.android.tools.build.apkzlib.bytestorage.ByteStorageFactory;
import com.android.tools.build.apkzlib.bytestorage.ChunkBasedByteStorageFactory;
import com.android.tools.build.apkzlib.bytestorage.OverflowToDiskByteStorageFactory;
import com.android.tools.build.apkzlib.bytestorage.TemporaryDirectory;
import com.android.tools.build.apkzlib.zip.compress.DeflateExecutionCompressor;
import com.android.tools.build.apkzlib.zip.utils.ByteTracker;
import com.google.common.base.Supplier;

import java.util.zip.Deflater;

public class ZFileOptions {

  private ByteStorageFactory storageFactory;

  private Compressor compressor;

  private boolean noTimestamps;

  private AlignmentRule alignmentRule;

  private boolean coverEmptySpaceUsingExtraField;

  private boolean autoSortFiles;

  private boolean skipValidation;

  private Supplier<VerifyLog> verifyLogFactory;

  private boolean alwaysGenerateJarManifest;

  public ZFileOptions() {
    storageFactory =
        new ChunkBasedByteStorageFactory(
            new OverflowToDiskByteStorageFactory(TemporaryDirectory::newSystemTemporaryDirectory));
    compressor = new DeflateExecutionCompressor(Runnable::run, Deflater.DEFAULT_COMPRESSION);
    alignmentRule = AlignmentRules.compose();
    verifyLogFactory = VerifyLogs::devNull;

    // We set this to true because many utilities stream the zip and expect no space between entries
    // in the zip file.
    coverEmptySpaceUsingExtraField = true;
    skipValidation = false;
    // True by default for backwards compatibility.
    alwaysGenerateJarManifest = true;
  }

  public ByteStorageFactory getStorageFactory() {
    return storageFactory;
  }

  @Deprecated
  public ByteTracker getTracker() {
    return new ByteTracker();
  }

  public ZFileOptions setStorageFactory(ByteStorageFactory storage) {
    this.storageFactory = storage;
    return this;
  }

  public Compressor getCompressor() {
    return compressor;
  }

  public ZFileOptions setCompressor(Compressor compressor) {
    this.compressor = compressor;
    return this;
  }

  public boolean getNoTimestamps() {
    return noTimestamps;
  }

  public ZFileOptions setNoTimestamps(boolean noTimestamps) {
    this.noTimestamps = noTimestamps;
    return this;
  }

  public AlignmentRule getAlignmentRule() {
    return alignmentRule;
  }

  public ZFileOptions setAlignmentRule(AlignmentRule alignmentRule) {
    this.alignmentRule = alignmentRule;
    return this;
  }

  public boolean getCoverEmptySpaceUsingExtraField() {
    return coverEmptySpaceUsingExtraField;
  }

  public ZFileOptions setCoverEmptySpaceUsingExtraField(boolean coverEmptySpaceUsingExtraField) {
    this.coverEmptySpaceUsingExtraField = coverEmptySpaceUsingExtraField;
    return this;
  }

  public boolean getAutoSortFiles() {
    return autoSortFiles;
  }

  public ZFileOptions setAutoSortFiles(boolean autoSortFiles) {
    this.autoSortFiles = autoSortFiles;
    return this;
  }

  public ZFileOptions setVerifyLogFactory(Supplier<VerifyLog> verifyLogFactory) {
    this.verifyLogFactory = verifyLogFactory;
    return this;
  }

  public Supplier<VerifyLog> getVerifyLogFactory() {
    return verifyLogFactory;
  }

  public ZFileOptions setSkipValidation(boolean skipValidation) {
    this.skipValidation = skipValidation;
    return this;
  }

  public boolean getSkipValidation() {
    return skipValidation;
  }

  public ZFileOptions setAlwaysGenerateJarManifest(boolean alwaysGenerateJarManifest) {
    this.alwaysGenerateJarManifest = alwaysGenerateJarManifest;
    return this;
  }

  public boolean getAlwaysGenerateJarManifest() {
    return alwaysGenerateJarManifest;
  }
}