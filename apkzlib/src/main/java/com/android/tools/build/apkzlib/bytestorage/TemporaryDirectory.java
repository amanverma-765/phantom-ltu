package com.android.tools.build.apkzlib.bytestorage;

import com.google.common.annotations.VisibleForTesting;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface TemporaryDirectory extends Closeable {

  File newFile() throws IOException;

  @VisibleForTesting // private otherwise.
  File getDirectory();

  static TemporaryDirectory newSystemTemporaryDirectory() throws IOException {
    Path tempDir = Files.createTempDirectory("tempdir_");
    TemporaryFile tempDirFile = new TemporaryFile(tempDir.toFile());
    return new TemporaryDirectory() {
      @Override
      public File newFile() throws IOException {
        return Files.createTempFile(tempDir, "temp_", ".data").toFile();
      }

      @Override
      public File getDirectory() {
        return tempDir.toFile();
      }

      @Override
      public void close() throws IOException {
        tempDirFile.close();
      }
    };
  }

  static TemporaryDirectory fixed(File directory) {
    return new TemporaryDirectory() {
      @Override
      public File newFile() throws IOException {
        return Files.createTempFile(directory.toPath(), "temp_", ".data").toFile();
      }

      @Override
      public File getDirectory() {
        return directory;
      }

      @Override
      public void close() throws IOException {}
    };
  }
}