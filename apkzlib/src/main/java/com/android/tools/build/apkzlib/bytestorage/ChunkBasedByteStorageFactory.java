package com.android.tools.build.apkzlib.bytestorage;

import java.io.IOException;

import javax.annotation.Nullable;

public class ChunkBasedByteStorageFactory implements ByteStorageFactory {

  private final ByteStorageFactory delegate;

  @Nullable private final Long maxChunkSize;

  public ChunkBasedByteStorageFactory(ByteStorageFactory delegate) {
    this(delegate, /*maxChunkSize=*/ null);
  }

  public ChunkBasedByteStorageFactory(ByteStorageFactory delegate, @Nullable Long maxChunkSize) {
    this.delegate = delegate;
    this.maxChunkSize = maxChunkSize;
  }

  @Override
  public ByteStorage create() throws IOException {
    if (maxChunkSize == null) {
      return new ChunkBasedByteStorage(delegate.create());
    } else {
      return new ChunkBasedByteStorage(maxChunkSize, delegate.create());
    }
  }
}