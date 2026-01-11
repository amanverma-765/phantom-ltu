/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

import javax.annotation.Nullable;

abstract class ZipField {

  private final String name;

  protected final int offset;

  private final int size;

  @Nullable private final Long expected;

  private final Set<ZipFieldInvariant> invariants;

  ZipField(int offset, int size, String name, ZipFieldInvariant... invariants) {
    Preconditions.checkArgument(offset >= 0, "offset >= 0");
    Preconditions.checkArgument(
        size == 2 || size == 4 || size == 8,
        "size != 2 && size != 4 && size != 8");

    this.name = name;
    this.offset = offset;
    this.size = size;
    expected = null;
    this.invariants = Sets.newHashSet(invariants);
  }

  ZipField(int offset, int size, long expected, String name) {
    Preconditions.checkArgument(offset >= 0, "offset >= 0");
    Preconditions.checkArgument(
        size == 2 || size == 4 || size == 8,
        "size != 2 && size != 4 && size != 8");

    this.name = name;
    this.offset = offset;
    this.size = size;
    this.expected = expected;
    invariants = Sets.newHashSet();
  }

  private void checkVerifiesInvariants(long value) throws IOException {
    for (ZipFieldInvariant invariant : invariants) {
      if (!invariant.isValid(value)) {
        throw new IOException(
            "Value "
                + value
                + " of field "
                + name
                + " is invalid "
                + "(fails '"
                + invariant.getName()
                + "').");
      }
    }
  }

  void skip(ByteBuffer bytes) throws IOException {
    if (bytes.remaining() < size) {
      throw new IOException(
          "Cannot skip field "
              + name
              + " because only "
              + bytes.remaining()
              + " remain in the buffer.");
    }

    bytes.position(bytes.position() + size);
  }

  long read(ByteBuffer bytes) throws IOException {
    if (bytes.remaining() < size) {
      throw new IOException(
          "Cannot skip field "
              + name
              + " because only "
              + bytes.remaining()
              + " remain in the buffer.");
    }

    bytes.order(ByteOrder.LITTLE_ENDIAN);

    long r;
    if (size == 2) {
      r = LittleEndianUtils.readUnsigned2Le(bytes);
    } else if (size == 4) {
      r = LittleEndianUtils.readUnsigned4Le(bytes);
    } else {
      r = LittleEndianUtils.readUnsigned8Le(bytes);
    }

    checkVerifiesInvariants(r);
    return r;
  }

  void verify(ByteBuffer bytes) throws IOException {
    verify(bytes, null);
  }

  void verify(ByteBuffer bytes, @Nullable VerifyLog verifyLog) throws IOException {
    Preconditions.checkState(expected != null, "expected == null");
    verify(bytes, expected, verifyLog);
  }

  void verify(ByteBuffer bytes, long expected) throws IOException {
    verify(bytes, expected, null);
  }

  void verify(ByteBuffer bytes, long expected, @Nullable VerifyLog verifyLog) throws IOException {
    checkVerifiesInvariants(expected);
    long r = read(bytes);
    if (r != expected) {
      String error =
          String.format(
              "Incorrect value for field '%s': value is %s but %s expected.", name, r, expected);

      if (verifyLog == null) {
        throw new IOException(error);
      } else {
        verifyLog.log(error);
      }
    }
  }

  void write(ByteBuffer output, long value) throws IOException {
    checkVerifiesInvariants(value);

    Preconditions.checkArgument(value >= 0, "value (%s) < 0", value);

    if (size == 2) {
      Preconditions.checkArgument(value <= 0x0000ffff, "value (%s) > 0x0000ffff", value);
      LittleEndianUtils.writeUnsigned2Le(output, Ints.checkedCast(value));
    } else if (size == 4) {
      Preconditions.checkArgument(
          value <= 0x00000000ffffffffL, "value (%s) > 0x00000000ffffffffL", value);
      LittleEndianUtils.writeUnsigned4Le(output, value);
    } else {
      Verify.verify(size == 8);
      LittleEndianUtils.writeUnsigned8Le(output, value);
    }
  }

  void write(ByteBuffer output) throws IOException {
    Preconditions.checkState(expected != null, "expected == null");
    write(output, expected);
  }

  int offset() {
    return offset;
  }

  int endOffset() {
    return offset + size;
  }

  static class F2 extends ZipField {

    F2(int offset, String name, ZipFieldInvariant... invariants) {
      super(offset, 2, name, invariants);
    }

    F2(int offset, long expected, String name) {
      super(offset, 2, expected, name);
    }
  }

  static class F4 extends ZipField {
    F4(int offset, String name, ZipFieldInvariant... invariants) {
      super(offset, 4, name, invariants);
    }

    F4(int offset, long expected, String name) {
      super(offset, 4, expected, name);
    }
  }

  static class F8 extends ZipField {

    F8(int offset, String name, ZipFieldInvariant... invariants) {
      super(offset, 8, name, invariants);
    }

    F8(int offset, long expected, String name) {
      super(offset, 8, expected, name);
    }
  }
}