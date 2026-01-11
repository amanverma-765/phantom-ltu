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
import com.google.common.io.Closer;

import java.io.Closeable;
import java.io.IOException;

public class ProcessedAndRawByteSources implements Closeable {

  private final CloseableByteSource processedSource;

  private final CloseableByteSource rawSource;

  public ProcessedAndRawByteSources(
      CloseableByteSource processedSource, CloseableByteSource rawSource) {
    this.processedSource = processedSource;
    this.rawSource = rawSource;
  }

  public CloseableByteSource getProcessedByteSource() {
    return processedSource;
  }

  public CloseableByteSource getRawByteSource() {
    return rawSource;
  }

  @Override
  public void close() throws IOException {
    Closer closer = Closer.create();
    closer.register(processedSource);
    closer.register(rawSource);
    closer.close();
  }
}