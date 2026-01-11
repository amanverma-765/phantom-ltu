/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.build.apkzlib.zip;

import com.android.tools.build.apkzlib.zip.utils.LittleEndianUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ExtraField {
  public static final ExtraField EMPTY = new ExtraField();

  static final int ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID = 0xd935;

  static final int LINKING_ENTRY_EXTRA_DATA_FIELD_HEADER_ID = 0x2333;

  @Nullable private final byte[] rawData;

  @Nullable private ImmutableList<Segment> segments;

  public ExtraField(byte[] rawData) {
    this.rawData = rawData;
    segments = null;
  }

  public ExtraField() {
    rawData = null;
    segments = ImmutableList.of();
  }

  public ExtraField(ImmutableList<Segment> segments) {
    rawData = null;
    this.segments = segments;
  }

  public ImmutableList<Segment> getSegments() throws IOException {
    if (segments == null) {
      parseSegments();
    }

    Preconditions.checkNotNull(segments);
    return segments;
  }

  @Nullable
  public Segment getSingleSegment(int headerId) throws IOException {
    List<Segment> found = new ArrayList<>();
    for (Segment s : getSegments()) {
      if (s.getHeaderId() == headerId) {
        found.add(s);
      }
    }

    if (found.isEmpty()) {
      return null;
    } else if (found.size() == 1) {
      return found.get(0);
    } else {
      throw new IOException(found.size() + " segments with header ID " + headerId + "found");
    }
  }

  private void parseSegments() throws IOException {
    Preconditions.checkNotNull(rawData);
    Preconditions.checkState(segments == null);

    List<Segment> segments = new ArrayList<>();
    ByteBuffer buffer = ByteBuffer.wrap(rawData);

    while (buffer.remaining() > 0) {
      int headerId = LittleEndianUtils.readUnsigned2Le(buffer);
      int dataSize = LittleEndianUtils.readUnsigned2Le(buffer);
      if (dataSize < 0) {
        throw new IOException(
            "Invalid data size for extra field segment with header ID "
                + headerId
                + ": "
                + dataSize);
      }

      byte[] data = new byte[dataSize];
      if (buffer.remaining() < dataSize) {
        throw new IOException(
            "Invalid data size for extra field segment with header ID "
                + headerId
                + ": "
                + dataSize
                + " (only "
                + buffer.remaining()
                + " bytes are available)");
      }
      buffer.get(data);

      SegmentFactory factory = identifySegmentFactory(headerId);
      Segment seg = factory.make(headerId, data);
      segments.add(seg);
    }

    this.segments = ImmutableList.copyOf(segments);
  }

  public int size() {
    if (rawData != null) {
      return rawData.length;
    } else {
      Preconditions.checkNotNull(segments);
      int sz = 0;
      for (Segment s : segments) {
        sz += s.size();
      }

      return sz;
    }
  }

  public void write(ByteBuffer out) throws IOException {
    if (rawData != null) {
      out.put(rawData);
    } else {
      Preconditions.checkNotNull(segments);
      for (Segment s : segments) {
        s.write(out);
      }
    }
  }

  private static SegmentFactory identifySegmentFactory(int headerId) {
    if (headerId == ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID) {
      return AlignmentSegment::new;
    }

    return RawDataSegment::new;
  }

  public interface Segment {

    int getHeaderId();

    int size();

    void write(ByteBuffer out) throws IOException;
  }

  interface SegmentFactory {

    Segment make(int headerId, byte[] data) throws IOException;
  }

  public static class RawDataSegment implements Segment {

    private final int headerId;

    private final byte[] data;

    RawDataSegment(int headerId, byte[] data) {
      this.headerId = headerId;
      this.data = data;
    }

    @Override
    public int getHeaderId() {
      return headerId;
    }

    @Override
    public void write(ByteBuffer out) throws IOException {
      LittleEndianUtils.writeUnsigned2Le(out, headerId);
      LittleEndianUtils.writeUnsigned2Le(out, data.length);
      out.put(data);
    }

    @Override
    public int size() {
      return 4 + data.length;
    }
  }

  public static class AlignmentSegment implements Segment {

    public static final int MINIMUM_SIZE = 6;

    private int alignment;

    private int padding;

    public AlignmentSegment(int alignment, int totalSize) {
      Preconditions.checkArgument(alignment > 0, "alignment <= 0");
      Preconditions.checkArgument(totalSize >= MINIMUM_SIZE, "totalSize < MINIMUM_SIZE");

      /*
       * We have 6 bytes of fixed data: header ID (2 bytes), data size (2 bytes), alignment
       * value (2 bytes).
       */
      this.alignment = alignment;
      padding = totalSize - MINIMUM_SIZE;
    }

    public AlignmentSegment(int headerId, byte[] data) throws IOException {
      Preconditions.checkArgument(headerId == ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID);

      ByteBuffer dataBuffer = ByteBuffer.wrap(data);
      alignment = LittleEndianUtils.readUnsigned2Le(dataBuffer);
      if (alignment <= 0) {
        throw new IOException("Invalid alignment in alignment field: " + alignment);
      }

      padding = data.length - 2;
    }

    @Override
    public void write(ByteBuffer out) throws IOException {
      LittleEndianUtils.writeUnsigned2Le(out, ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID);
      LittleEndianUtils.writeUnsigned2Le(out, padding + 2);
      LittleEndianUtils.writeUnsigned2Le(out, alignment);
      out.put(new byte[padding]);
    }

    @Override
    public int size() {
      return padding + 6;
    }

    @Override
    public int getHeaderId() {
      return ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID;
    }
  }

  public static class LinkingEntrySegment implements Segment {

    private final StoredEntry linkingEntry;
    private int dataOffset = -1;
    private long zipOffset = -1;

    public LinkingEntrySegment(StoredEntry linkingEntry) throws IOException {
      Preconditions.checkArgument(linkingEntry.isLinkingEntry(), "linkingEntry is not a linking entry");
      this.linkingEntry = linkingEntry;
    }

    @Override
    public int getHeaderId() {
      return LINKING_ENTRY_EXTRA_DATA_FIELD_HEADER_ID;
    }

    @Override
    public int size() {
      return linkingEntry.isDummyEntry() ? 0 : linkingEntry.getLocalHeaderSize() + 4;
    }

    public void setOffset(int dataOffset, long zipOffset) {
      this.dataOffset = dataOffset;
      this.zipOffset = zipOffset;
    }

    @Override
    public void write(ByteBuffer out) throws IOException {
      if (dataOffset < 0 || zipOffset < 0) {
        throw new IOException("linking entry has wrong offset");
      }
      if (!linkingEntry.isDummyEntry()) {
        LittleEndianUtils.writeUnsigned2Le(out, LINKING_ENTRY_EXTRA_DATA_FIELD_HEADER_ID);
        LittleEndianUtils.writeUnsigned2Le(out, linkingEntry.getLocalHeaderSize());
        var offset = out.position();
        linkingEntry.writeData(out, dataOffset - linkingEntry.getLocalHeaderSize() - offset);
        linkingEntry.replaceSourceFromZip(offset + zipOffset);
      } else {
        linkingEntry.replaceSourceFromZip(zipOffset + dataOffset + linkingEntry.getNestedOffset());
      }
    }
  }
}