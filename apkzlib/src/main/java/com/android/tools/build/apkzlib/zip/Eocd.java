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

import com.android.tools.build.apkzlib.utils.CachedSupplier;
import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

class Eocd {

  static final long MAX_TOTAL_RECORDS = 0xFFFFL;

  static final long MAX_CD_SIZE = 0xFFFFFFFFL;

  static final long MAX_CD_OFFSET = 0xFFFFFFFFL;

  private static final ZipField.F4 F_SIGNATURE = new ZipField.F4(0, 0x06054b50, "EOCD signature");

  private static final ZipField.F2 F_NUMBER_OF_DISK =
      new ZipField.F2(F_SIGNATURE.endOffset(), 0, "Number of this disk");

  private static final ZipField.F2 F_DISK_CD_START =
      new ZipField.F2(F_NUMBER_OF_DISK.endOffset(), 0, "Disk where CD starts");

  private static final ZipField.F2 F_RECORDS_DISK =
      new ZipField.F2(
          F_DISK_CD_START.endOffset(), "Record on disk count", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_RECORDS_TOTAL =
      new ZipField.F2(
          F_RECORDS_DISK.endOffset(),
          "Total records",
          new ZipFieldInvariantNonNegative(),
          new ZipFieldInvariantMaxValue(Integer.MAX_VALUE));

  @VisibleForTesting
  static final ZipField.F4 F_CD_SIZE =
      new ZipField.F4(
          F_RECORDS_TOTAL.endOffset(), "Directory size", new ZipFieldInvariantNonNegative());

  @VisibleForTesting
  static final ZipField.F4 F_CD_OFFSET =
      new ZipField.F4(
          F_CD_SIZE.endOffset(), "Directory offset", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_COMMENT_SIZE =
      new ZipField.F2(
          F_CD_OFFSET.endOffset(), "File comment size", new ZipFieldInvariantNonNegative());

  private final long totalRecords;

  private final long directoryOffset;

  private final long directorySize;

  private final byte[] comment;

  private final CachedSupplier<byte[]> byteSupplier;

  Eocd(ByteBuffer bytes) throws IOException {

    /*
     * Read the EOCD record.
     */
    F_SIGNATURE.verify(bytes);
    F_NUMBER_OF_DISK.verify(bytes);
    F_DISK_CD_START.verify(bytes);
    long totalRecords1 = F_RECORDS_DISK.read(bytes);
    long totalRecords2 = F_RECORDS_TOTAL.read(bytes);
    long directorySize = F_CD_SIZE.read(bytes);
    long directoryOffset = F_CD_OFFSET.read(bytes);
    int commentSize = Ints.checkedCast(F_COMMENT_SIZE.read(bytes));

    /*
     * Some sanity checks.
     */
    if (totalRecords1 != totalRecords2) {
      throw new IOException(
          "Zip states records split in multiple disks, which is not " + "supported.");
    }

    Verify.verify(totalRecords1 <= Integer.MAX_VALUE);

    totalRecords = Ints.checkedCast(totalRecords1);
    this.directorySize = directorySize;
    this.directoryOffset = directoryOffset;

    if (bytes.remaining() < commentSize) {
      throw new IOException(
          "Corrupt EOCD record: not enough data for comment (comment "
              + "size is "
              + commentSize
              + ").");
    }

    comment = new byte[commentSize];
    bytes.get(comment);
    byteSupplier = new CachedSupplier<>(this::computeByteRepresentation);
  }

  Eocd(long totalRecords, long directoryOffset, long directorySize, byte[] comment) {
    Preconditions.checkArgument(totalRecords >= 0, "totalRecords < 0");
    Preconditions.checkArgument(directoryOffset >= 0, "directoryOffset < 0");
    Preconditions.checkArgument(directorySize >= 0, "directorySize < 0");

    this.totalRecords = totalRecords;
    this.directoryOffset = directoryOffset;
    this.directorySize = directorySize;
    this.comment = comment;
    byteSupplier = new CachedSupplier<>(this::computeByteRepresentation);
  }

  long getTotalRecords() {
    return totalRecords;
  }

  long getDirectoryOffset() {
    return directoryOffset;
  }

  long getDirectorySize() {
    return directorySize;
  }

  long getEocdSize() {
    return (long) F_COMMENT_SIZE.endOffset() + comment.length;
  }

  byte[] toBytes() throws IOException {
    return byteSupplier.get();
  }

  byte[] getComment() {
    byte[] commentCopy = new byte[comment.length];
    System.arraycopy(comment, 0, commentCopy, 0, comment.length);
    return commentCopy;
  }

  private byte[] computeByteRepresentation() {
    ByteBuffer out = ByteBuffer.allocate(F_COMMENT_SIZE.endOffset() + comment.length);

    try {
      F_SIGNATURE.write(out);
      F_NUMBER_OF_DISK.write(out);
      F_DISK_CD_START.write(out);
      F_RECORDS_DISK.write(out, totalRecords);
      F_RECORDS_TOTAL.write(out, totalRecords);
      F_CD_SIZE.write(out, directorySize);
      F_CD_OFFSET.write(out, directoryOffset);
      F_COMMENT_SIZE.write(out, comment.length);
      out.put(comment);

      return out.array();
    } catch (IOException e) {
      throw new IOExceptionWrapper(e);
    }
  }
}