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

package com.android.tools.build.apkzlib.zip.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.annotation.Nullable;

public class CloseableDelegateByteSource extends CloseableByteSource {

  @Nullable private ByteSource inner;

  private final long mSize;

  public CloseableDelegateByteSource(ByteSource inner, long size) {
    this.inner = inner;
    mSize = size;
  }

  private synchronized ByteSource get() {
    if (inner == null) {
      throw new ByteSourceDisposedException();
    }

    return inner;
  }

  @Override
  protected synchronized void innerClose() throws IOException {
    if (inner == null) {
      return;
    }

    inner = null;
  }

  public long sizeNoException() {
    return mSize;
  }

  @Override
  public CharSource asCharSource(Charset charset) {
    return get().asCharSource(charset);
  }

  @Override
  public InputStream openBufferedStream() throws IOException {
    return get().openBufferedStream();
  }

  @Override
  public ByteSource slice(long offset, long length) {
    return get().slice(offset, length);
  }

  @Override
  public boolean isEmpty() throws IOException {
    return get().isEmpty();
  }

  @Override
  public long size() throws IOException {
    return get().size();
  }

  @Override
  public long copyTo(OutputStream output) throws IOException {
    return get().copyTo(output);
  }

  @Override
  public long copyTo(ByteSink sink) throws IOException {
    return get().copyTo(sink);
  }

  @Override
  public byte[] read() throws IOException {
    return get().read();
  }

  @Override
  public <T> T read(ByteProcessor<T> processor) throws IOException {
    return get().read(processor);
  }

  @Override
  public HashCode hash(HashFunction hashFunction) throws IOException {
    return get().hash(hashFunction);
  }

  @Override
  public boolean contentEquals(ByteSource other) throws IOException {
    return get().contentEquals(other);
  }

  @Override
  public InputStream openStream() throws IOException {
    return get().openStream();
  }

  private static class ByteSourceDisposedException extends RuntimeException {

    private ByteSourceDisposedException() {
      super(
          "Byte source was created by a ByteTracker and is now disposed. If you see "
              + "this message, then there is a bug.");
    }
  }
}