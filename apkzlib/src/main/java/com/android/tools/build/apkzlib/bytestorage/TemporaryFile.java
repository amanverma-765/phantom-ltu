package com.android.tools.build.apkzlib.bytestorage;

import com.google.common.base.Preconditions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class TemporaryFile implements Closeable {

  private boolean deleted;

  private final File file;

  public TemporaryFile(File file) {
    deleted = false;
    this.file = file;
  }

  public File getFile() {
    Preconditions.checkState(!deleted, "File already deleted");
    return file;
  }

  @Override
  public void close() throws IOException {
    if (deleted) {
      return;
    }

    deleted = true;

    deleteFile(file);
  }

  private void deleteFile(File file) throws IOException {
    if (file.isDirectory()) {
      File[] contents = file.listFiles();
      if (contents != null) {
        for (File subFile : contents) {
          deleteFile(subFile);
        }
      }
    }

    if (file.exists() && !file.delete()) {
      throw new IOException("Failed to delete '" + file.getAbsolutePath() + "'");
    }
  }
}