package com.android.tools.build.apkzlib.bytestorage;

import com.android.tools.build.apkzlib.zip.utils.CloseableDelegateByteSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

class TemporaryFileCloseableByteSource extends CloseableDelegateByteSource {

  private final TemporaryFile temporaryFile;

  private final Runnable closeCallback;

  TemporaryFileCloseableByteSource(File file, Runnable closeCallback) {
    super(Files.asByteSource(file), file.length());
    temporaryFile = new TemporaryFile(file);
    this.closeCallback = closeCallback;
  }

  @Override
  protected synchronized void innerClose() throws IOException {
    super.innerClose();
    temporaryFile.close();
    closeCallback.run();
  }
}