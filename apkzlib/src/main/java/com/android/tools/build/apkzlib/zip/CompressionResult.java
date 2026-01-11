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

import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;

public class CompressionResult {

  private final CompressionMethod compressionMethod;

  private final CloseableByteSource source;

  private final long mSize;

  public CompressionResult(CloseableByteSource source, CompressionMethod method, long size) {
    compressionMethod = method;
    this.source = source;
    mSize = size;
  }

  public CompressionMethod getCompressionMethod() {
    return compressionMethod;
  }

  public CloseableByteSource getSource() {
    return source;
  }

  public long getSize() {
    return mSize;
  }
}
