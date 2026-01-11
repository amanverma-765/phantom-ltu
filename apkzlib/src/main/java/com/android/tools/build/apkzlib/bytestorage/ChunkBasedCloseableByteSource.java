package com.android.tools.build.apkzlib.bytestorage;

import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.android.tools.build.apkzlib.zip.utils.CloseableDelegateByteSource;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Closer;

import java.io.IOException;
import java.util.List;

class ChunkBasedCloseableByteSource extends CloseableDelegateByteSource {

  private final ImmutableList<CloseableByteSource> sources;

  ChunkBasedCloseableByteSource(List<CloseableByteSource> sources) throws IOException {
    super(ByteSource.concat(sources), sumSizes(sources));
    this.sources = ImmutableList.copyOf(sources);
  }

  private static long sumSizes(List<CloseableByteSource> sources) throws IOException {
    long sum = 0;
    for (CloseableByteSource source : sources) {
      sum += source.size();
    }

    return sum;
  }

  @Override
  protected synchronized void innerClose() throws IOException {
    try (Closer closer = Closer.create()) {
      for (CloseableByteSource source : sources) {
        closer.register(source);
      }
    }
  }
}