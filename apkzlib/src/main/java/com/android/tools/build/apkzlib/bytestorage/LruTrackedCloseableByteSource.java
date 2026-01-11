package com.android.tools.build.apkzlib.bytestorage;

import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

class LruTrackedCloseableByteSource extends SwitchableDelegateCloseableByteSource {
  private final LruTracker<LruTrackedCloseableByteSource> tracker;

  private boolean tracking;

  private boolean closed;

  LruTrackedCloseableByteSource(
      CloseableByteSource delegate, LruTracker<LruTrackedCloseableByteSource> tracker)
      throws IOException {
    super(delegate);
    this.tracker = tracker;
    tracker.track(this);
    tracking = true;
    closed = false;
  }

  @Override
  public synchronized InputStream openStream() throws IOException {
    Preconditions.checkState(!closed);
    if (tracking) {
      tracker.access(this);
    }

    return super.openStream();
  }

  @Override
  protected synchronized void innerClose() throws IOException {
    closed = true;

    untrack();
    super.innerClose();
  }

  private synchronized void untrack() {
    if (tracking) {
      tracking = false;
      tracker.untrack(this);
    }
  }

  synchronized void move(ByteStorage diskStorage) throws IOException {
    if (closed) {
      return;
    }

    CloseableByteSource diskSource = diskStorage.fromSource(this);
    untrack();
    switchSource(diskSource);
  }
}