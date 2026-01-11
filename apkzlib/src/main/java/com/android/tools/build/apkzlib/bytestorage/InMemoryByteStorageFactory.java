package com.android.tools.build.apkzlib.bytestorage;

import java.io.IOException;

public class InMemoryByteStorageFactory implements ByteStorageFactory {

  @Override
  public ByteStorage create() throws IOException {
    return new InMemoryByteStorage();
  }
}
