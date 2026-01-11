package com.android.tools.build.apkzlib.bytestorage;

import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;

public class OverflowToDiskByteStorage implements ByteStorage {

  private static final long DEFAULT_MEMORY_CACHE_BYTES = 50 * 1024 * 1024;

  private final InMemoryByteStorage memoryStorage;

  @VisibleForTesting // private otherwise.
  final TemporaryDirectoryStorage diskStorage;

  private final LruTracker<LruTrackedCloseableByteSource> memorySourcesTracker;

  private final long memoryCacheSize;

  private long maxBytesUsed;

  public OverflowToDiskByteStorage(TemporaryDirectoryFactory temporaryDirectoryFactory)
      throws IOException {
    this(DEFAULT_MEMORY_CACHE_BYTES, temporaryDirectoryFactory);
  }

  public OverflowToDiskByteStorage(
      long memoryCacheSize, TemporaryDirectoryFactory temporaryDirectoryFactory)
      throws IOException {
    memoryStorage = new InMemoryByteStorage();
    diskStorage = new TemporaryDirectoryStorage(temporaryDirectoryFactory);
    this.memoryCacheSize = memoryCacheSize;
    this.memorySourcesTracker = new LruTracker<>();
  }

  @Override
  public CloseableByteSource fromStream(InputStream stream) throws IOException {
    CloseableByteSource memSource =
        new LruTrackedCloseableByteSource(memoryStorage.fromStream(stream), memorySourcesTracker);
    checkMaxUsage();
    reviewSources();
    return memSource;
  }

  @Override
  public CloseableByteSourceFromOutputStreamBuilder makeBuilder() throws IOException {
    CloseableByteSourceFromOutputStreamBuilder memBuilder = memoryStorage.makeBuilder();
    return new AbstractCloseableByteSourceFromOutputStreamBuilder() {
      @Override
      protected void doWrite(byte[] b, int off, int len) throws IOException {
        memBuilder.write(b, off, len);
      }

      @Override
      protected CloseableByteSource doBuild() throws IOException {
        CloseableByteSource memSource =
            new LruTrackedCloseableByteSource(memBuilder.build(), memorySourcesTracker);
        checkMaxUsage();
        reviewSources();
        return memSource;
      }
    };
  }

  @Override
  public CloseableByteSource fromSource(ByteSource source) throws IOException {
    CloseableByteSource memSource =
        new LruTrackedCloseableByteSource(memoryStorage.fromSource(source), memorySourcesTracker);
    checkMaxUsage();
    reviewSources();
    return memSource;
  }

  @Override
  public synchronized long getBytesUsed() {
    return memoryStorage.getBytesUsed() + diskStorage.getBytesUsed();
  }

  @Override
  public synchronized long getMaxBytesUsed() {
    return maxBytesUsed;
  }

  private synchronized void checkMaxUsage() {
    if (getBytesUsed() > maxBytesUsed) {
      maxBytesUsed = getBytesUsed();
    }
  }

  private synchronized void reviewSources() throws IOException {
    // Move data from memory to disk until we have at most memoryCacheSize bytes in memory.
    while (memoryStorage.getBytesUsed() > memoryCacheSize) {
      LruTrackedCloseableByteSource last = memorySourcesTracker.last();
      if (last != null) {
        LruTrackedCloseableByteSource lastSource = last;
        lastSource.move(diskStorage);
      }
    }
  }

  public long getMemoryBytesUsed() {
    return memoryStorage.getBytesUsed();
  }

  public long getMaxMemoryBytesUsed() {
    return memoryStorage.getMaxBytesUsed();
  }

  public long getDiskBytesUsed() {
    return diskStorage.getBytesUsed();
  }

  public long getMaxDiskBytesUsed() {
    return diskStorage.getMaxBytesUsed();
  }

  @Override
  public void close() throws IOException {
    memoryStorage.close();
    diskStorage.close();
  }
}