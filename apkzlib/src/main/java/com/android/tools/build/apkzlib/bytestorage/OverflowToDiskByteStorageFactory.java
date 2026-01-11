package com.android.tools.build.apkzlib.bytestorage;

import java.io.IOException;

import javax.annotation.Nullable;

public class OverflowToDiskByteStorageFactory implements ByteStorageFactory {

  @Nullable private final Long memoryCacheSizeInBytes;

  private final TemporaryDirectoryFactory temporaryDirectoryFactory;

  public OverflowToDiskByteStorageFactory(TemporaryDirectoryFactory temporaryDirectoryFactory) {
    this(null, temporaryDirectoryFactory);
  }

  public OverflowToDiskByteStorageFactory(
      Long memoryCacheSizeInBytes, TemporaryDirectoryFactory temporaryDirectoryFactory) {
    this.memoryCacheSizeInBytes = memoryCacheSizeInBytes;
    this.temporaryDirectoryFactory = temporaryDirectoryFactory;
  }

  @Override
  public ByteStorage create() throws IOException {
    if (memoryCacheSizeInBytes == null) {
      return new OverflowToDiskByteStorage(temporaryDirectoryFactory);
    } else {
      return new OverflowToDiskByteStorage(memoryCacheSizeInBytes, temporaryDirectoryFactory);
    }
  }
}