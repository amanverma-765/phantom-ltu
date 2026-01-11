package com.android.tools.build.apkzlib.bytestorage;

import java.io.IOException;

public interface ByteStorageFactory {

  ByteStorage create() throws IOException;
}
