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

package com.android.tools.build.apkzlib.zfile;

import com.android.tools.build.apkzlib.sign.SigningOptions;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApkCreatorFactory {

  ApkCreator make(CreationData creationData);

  @AutoValue
  abstract class CreationData {

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setApkPath(@Nonnull File apkPath);

      public abstract Builder setSigningOptions(@Nonnull SigningOptions signingOptions);

      public abstract Builder setBuiltBy(@Nullable String buildBy);

      public abstract Builder setCreatedBy(@Nullable String createdBy);

      public abstract Builder setNativeLibrariesPackagingMode(
          NativeLibrariesPackagingMode packagingMode);

      public abstract Builder setNoCompressPredicate(Predicate<String> predicate);

      public abstract Builder setIncremental(boolean incremental);

      abstract CreationData autoBuild();

      public CreationData build() {
        CreationData data = autoBuild();
        Preconditions.checkArgument(data.getApkPath() != null, "Output apk path is not set");
        return data;
      }
    }

    public static Builder builder() {
      return new AutoValue_ApkCreatorFactory_CreationData.Builder()
          .setBuiltBy(null)
          .setCreatedBy(null)
          .setNoCompressPredicate(s -> false)
          .setIncremental(false);
    }

    public abstract File getApkPath();

    @Nonnull
    public abstract Optional<SigningOptions> getSigningOptions();

    @Nullable
    public abstract String getBuiltBy();

    @Nullable
    public abstract String getCreatedBy();

    public abstract NativeLibrariesPackagingMode getNativeLibrariesPackagingMode();

    public abstract Predicate<String> getNoCompressPredicate();

    public abstract boolean isIncremental();
  }
}