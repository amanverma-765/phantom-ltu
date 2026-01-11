package com.android.tools.build.apkzlib.bytestorage;

import java.io.File;
import java.io.IOException;

public interface TemporaryDirectoryFactory {

  TemporaryDirectory make() throws IOException;

  static TemporaryDirectoryFactory fixed(File directory) {
    return () -> TemporaryDirectory.fixed(directory);
  }
}
