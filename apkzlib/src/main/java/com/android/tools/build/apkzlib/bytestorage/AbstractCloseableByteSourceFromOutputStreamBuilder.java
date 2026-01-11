package com.android.tools.build.apkzlib.bytestorage;

import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.google.common.base.Preconditions;

import java.io.IOException;

abstract class AbstractCloseableByteSourceFromOutputStreamBuilder
    extends CloseableByteSourceFromOutputStreamBuilder {

  private final byte[] tempByte;

  private boolean closed;

  private boolean built;

  AbstractCloseableByteSourceFromOutputStreamBuilder() {
    tempByte = new byte[1];
    closed = false;
    built = false;
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    Preconditions.checkState(!closed);
    doWrite(b, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    tempByte[0] = (byte) b;
    write(tempByte, 0, 1);
  }

  @Override
  public void close() throws IOException {
    closed = true;
  }

  @Override
  public CloseableByteSource build() throws IOException {
    Preconditions.checkState(!built);
    closed = true;
    built = true;

    return doBuild();
  }

  protected abstract void doWrite(byte[] b, int off, int len) throws IOException;

  protected abstract CloseableByteSource doBuild() throws IOException;
}