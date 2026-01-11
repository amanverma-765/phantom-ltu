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


public class CentralDirectoryHeaderCompressInfo {

  public static final long VERSION_WITH_STORE_FILES_ONLY = 10L;

  public static final long VERSION_WITH_DIRECTORIES_AND_DEFLATE = 20L;

  public static final long VERSION_WITH_ZIP64_EXTENSIONS = 45L;

  public static final long VERSION_WITH_CENTRAL_FILE_ENCRYPTION = 62L;

  private final CompressionMethod method;

  private final long compressedSize;

  private final long versionExtract;

  public CentralDirectoryHeaderCompressInfo(
      CompressionMethod method, long compressedSize, long versionToExtract) {
    this.method = method;
    this.compressedSize = compressedSize;
    versionExtract = versionToExtract;
  }

  public CentralDirectoryHeaderCompressInfo(
      CentralDirectoryHeader header, CompressionMethod method, long compressedSize) {
    this.method = method;
    this.compressedSize = compressedSize;

   if (header.getName().endsWith("/") || method == CompressionMethod.DEFLATE) {
      /*
       * Directories and compressed files only in version 2.0.
       */
      versionExtract = VERSION_WITH_DIRECTORIES_AND_DEFLATE;
    } else {
      versionExtract = VERSION_WITH_STORE_FILES_ONLY;
    }
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public CompressionMethod getMethod() {
    return method;
  }

  long getVersionExtract() {
    return versionExtract;
  }
}
