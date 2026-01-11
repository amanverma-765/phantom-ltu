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

import com.android.tools.build.apkzlib.utils.CachedSupplier;
import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

class Zip64EocdLocator {
  private static final ZipField.F4 F_SIGNATURE =
      new ZipField.F4(0, 0x07064b50, "Zip64 EOCD Locator signature");

  private static final ZipField.F4 F_NUMBER_OF_DISK =
      new ZipField.F4(F_SIGNATURE.endOffset(), 0, "Number of disk with Zip64 EOCD");

  private static final ZipField.F8 F_Z64_EOCD_OFFSET =
      new ZipField.F8(
          F_NUMBER_OF_DISK.endOffset(),
          "Offset of Zip64 EOCD",
          new ZipFieldInvariantNonNegative());

  private static final ZipField.F4 F_TOTAL_NUMBER_OF_DISKS =
      new ZipField.F4(
          F_Z64_EOCD_OFFSET.endOffset(), 0,"Total number of disks");


  public static final int LOCATOR_SIZE = F_TOTAL_NUMBER_OF_DISKS.endOffset();

  private final long z64EocdOffset;

  private final CachedSupplier<byte[]> byteSupplier;

  Zip64EocdLocator(ByteBuffer bytes) throws IOException {
    F_SIGNATURE.verify(bytes);
    F_NUMBER_OF_DISK.verify(bytes);
    long z64EocdOffset = F_Z64_EOCD_OFFSET.read(bytes);
    F_TOTAL_NUMBER_OF_DISKS.verify(bytes);

    Verify.verify(z64EocdOffset >= 0);
    this.z64EocdOffset = z64EocdOffset;
    byteSupplier = new CachedSupplier<>(this::computeByteRepresentation);
  }

  Zip64EocdLocator(long z64EocdOffset) {
    Preconditions.checkArgument(z64EocdOffset >= 0, "z64EocdOffset < 0");

    this.z64EocdOffset = z64EocdOffset;
    byteSupplier = new CachedSupplier<>(this::computeByteRepresentation);
  }

  long getZ64EocdOffset() {
    return z64EocdOffset;
  }

  long getSize() {
    return F_TOTAL_NUMBER_OF_DISKS.endOffset();
  }

  byte[] toBytes() throws IOException {
    return byteSupplier.get();
  }

  private byte[] computeByteRepresentation() {
    ByteBuffer out = ByteBuffer.allocate(F_TOTAL_NUMBER_OF_DISKS.endOffset());

    try {
      F_SIGNATURE.write(out);
      F_NUMBER_OF_DISK.write(out);
      F_Z64_EOCD_OFFSET.write(out, z64EocdOffset);
      F_TOTAL_NUMBER_OF_DISKS.write(out);

      return out.array();
    } catch (IOException e) {
      throw new IOExceptionWrapper(e);
    }
  }
}