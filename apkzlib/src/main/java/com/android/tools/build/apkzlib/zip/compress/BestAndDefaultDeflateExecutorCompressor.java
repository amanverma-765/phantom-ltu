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

package com.android.tools.build.apkzlib.zip.compress;

import com.android.tools.build.apkzlib.bytestorage.ByteStorage;
import com.android.tools.build.apkzlib.zip.CompressionResult;
import com.android.tools.build.apkzlib.zip.utils.ByteTracker;
import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.google.common.base.Preconditions;

import java.util.concurrent.Executor;
import java.util.zip.Deflater;

public class BestAndDefaultDeflateExecutorCompressor extends ExecutorCompressor {

  private final DeflateExecutionCompressor defaultDeflater;

  private final DeflateExecutionCompressor bestDeflater;

  private final double minRatio;

  public BestAndDefaultDeflateExecutorCompressor(Executor executor, double minRatio) {
    super(executor);

    Preconditions.checkArgument(minRatio >= 0.0, "minRatio < 0.0");
    Preconditions.checkArgument(minRatio <= 1.0, "minRatio > 1.0");

    defaultDeflater = new DeflateExecutionCompressor(executor, Deflater.DEFAULT_COMPRESSION);
    bestDeflater = new DeflateExecutionCompressor(executor, Deflater.BEST_COMPRESSION);
    this.minRatio = minRatio;
  }

  @Deprecated
  public BestAndDefaultDeflateExecutorCompressor(
      Executor executor, ByteTracker tracker, double minRatio) {
    this(executor, minRatio);
  }

  @Override
  protected CompressionResult immediateCompress(CloseableByteSource source, ByteStorage storage)
      throws Exception {
    CompressionResult defaultResult = defaultDeflater.immediateCompress(source, storage);
    CompressionResult bestResult = bestDeflater.immediateCompress(source, storage);

    double sizeRatio = bestResult.getSize() / (double) defaultResult.getSize();
    if (sizeRatio >= minRatio) {
      return defaultResult;
    } else {
      return bestResult;
    }
  }
}