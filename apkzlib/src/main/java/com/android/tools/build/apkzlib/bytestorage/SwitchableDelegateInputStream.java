package com.android.tools.build.apkzlib.bytestorage;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;

class SwitchableDelegateInputStream extends InputStream {

  private InputStream delegate;

  private long currentOffset;

  @VisibleForTesting // private otherwise.
  boolean endOfStreamReached;

  private long needsSkipping;

  SwitchableDelegateInputStream(InputStream delegate) {
    this.delegate = delegate;
    currentOffset = 0;
    endOfStreamReached = false;
    needsSkipping = 0;
  }

  private void skipDataIfNeeded() throws IOException {
    while (needsSkipping > 0) {
      long skipped = delegate.skip(needsSkipping);
      if (skipped == 0) {
        throw new IOException("Skipping InputStream after switching failed");
      }

      needsSkipping -= skipped;
    }
  }

  private int increaseOffset(int amount) {
    return (int) increaseOffset((long) amount);
  }

  private long increaseOffset(long amount) {
    if (amount > 0) {
      currentOffset += amount;
    }

    if (amount == -1) {
      endOfStreamReached = true;
    }

    return amount;
  }

  @Override
  public synchronized int read(byte[] b) throws IOException {
    if (endOfStreamReached) {
      return -1;
    }

    skipDataIfNeeded();
    return increaseOffset(delegate.read(b));
  }

  @Override
  public synchronized int read(byte[] b, int off, int len) throws IOException {
    if (endOfStreamReached) {
      return -1;
    }

    skipDataIfNeeded();
    return increaseOffset(delegate.read(b, off, len));
  }

  @Override
  public synchronized int read() throws IOException {
    if (endOfStreamReached) {
      return -1;
    }

    skipDataIfNeeded();
    int r = delegate.read();
    if (r == -1) {
      endOfStreamReached = true;
    } else {
      increaseOffset(1);
    }

    return r;
  }

  @Override
  public synchronized long skip(long n) throws IOException {
    if (endOfStreamReached) {
      return 0;
    }

    skipDataIfNeeded();
    return increaseOffset(delegate.skip(n));
  }

  @Override
  public synchronized int available() throws IOException {
    if (endOfStreamReached) {
      return 0;
    }

    skipDataIfNeeded();
    return delegate.available();
  }

  @Override
  public synchronized void close() throws IOException {
    endOfStreamReached = true;
    delegate.close();
  }

  @Override
  public void mark(int readlimit) {
    // We don't support marking.
  }

  @Override
  public void reset() throws IOException {
    throw new IOException("Mark not supported");
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  synchronized void switchStream(InputStream newStream) throws IOException {
    if (newStream == delegate) {
      return;
    }

    try (InputStream oldDelegate = delegate) {
      delegate = newStream;
      needsSkipping = currentOffset;
    }
  }
}