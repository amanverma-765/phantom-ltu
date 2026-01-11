/*
 * Copyright (C) 2018 The Android Open Source Project
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
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class Zip64ExtensibleDataSector {

  @Nullable private final byte[] rawData;

  @Nullable private ImmutableList<Z64SpecialPurposeData> fields;

  public Zip64ExtensibleDataSector(byte[] rawData) {
    this.rawData = rawData;
    fields = null;
  }

  public Zip64ExtensibleDataSector() {
    rawData = null;
    fields = ImmutableList.of();
  }

  public Zip64ExtensibleDataSector(ImmutableList<Z64SpecialPurposeData> fields) {
    rawData = null;
    this.fields = fields;
  }

  int size() {
    if (rawData != null) {
      return rawData.length;
    } else {
      Preconditions.checkNotNull(fields);
      int sumSizes = 0;
      for (Z64SpecialPurposeData data : fields){
        sumSizes += data.size();
      }
      return sumSizes;
    }
  }

  void write(ByteBuffer out) throws IOException {
    if (rawData != null) {
      out.put(rawData);
    } else {
      Preconditions.checkNotNull(fields);
      for (Z64SpecialPurposeData data : fields) {
        data.write(out);
      }
    }
  }

  public ImmutableList<Z64SpecialPurposeData> getFields() throws IOException {
    if (fields == null) {
      parseData();
    }

    Preconditions.checkNotNull(fields);
    return fields;
  }

  private void parseData() throws IOException {
    Preconditions.checkNotNull(rawData);
    Preconditions.checkState(fields == null);

    List<Z64SpecialPurposeData> fields = new ArrayList<>();
    ByteBuffer buffer = ByteBuffer.wrap(rawData);

    while (buffer.remaining() > 0) {
      int headerId = LittleEndianUtils.readUnsigned2Le(buffer);
      long dataSize = LittleEndianUtils.readUnsigned4Le(buffer);

      byte[] data = new byte[Ints.checkedCast(dataSize)];
      if (dataSize < 0) {
        throw new IOException(
            "Invalid data size for special purpose data with header ID "
                + headerId
                + ": "
                + dataSize);
      }
      buffer.get(data);

      SpecialPurposeDataFactory factory = RawSpecialPurposeData::new;
      Z64SpecialPurposeData spd = factory.make(headerId, data);
      fields.add(spd);
    }
    this.fields = ImmutableList.copyOf(fields);
  }

  public interface Z64SpecialPurposeData {

    int PREFIX_LENGTH = 6;

    int getHeaderId();

    int size();

    void write(ByteBuffer out) throws IOException;
  }

  public  interface SpecialPurposeDataFactory {

    Z64SpecialPurposeData make(int headerId, byte[] data) throws IOException;
  }

  public static class RawSpecialPurposeData implements Z64SpecialPurposeData {

    private final int headerId;

    private final byte[] data;

    RawSpecialPurposeData(int headerId, byte[] data) {
      this.headerId = headerId;
      this.data = data;
    }

    @Override
    public int getHeaderId() {
      return headerId;
    }

    @Override
    public int size() {
      return PREFIX_LENGTH + data.length;
    }

    @Override
    public void write(ByteBuffer out) throws IOException {
      LittleEndianUtils.writeUnsigned2Le(out, headerId);
      LittleEndianUtils.writeUnsigned4Le(out, data.length);
      out.put(data);
    }
  }
}