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

import com.android.tools.build.apkzlib.bytestorage.ByteStorage;
import com.android.tools.build.apkzlib.utils.CachedSupplier;
import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.android.tools.build.apkzlib.zip.utils.MsDosDateTimeUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

class CentralDirectory {

  private static final ZipField.F4 F_SIGNATURE = new ZipField.F4(0, 0x02014b50, "Signature");

  private static final ZipField.F2 F_MADE_BY =
      new ZipField.F2(F_SIGNATURE.endOffset(), "Made by", new ZipFieldInvariantNonNegative());

  @VisibleForTesting
  static final ZipField.F2 F_VERSION_EXTRACT =
      new ZipField.F2(
          F_MADE_BY.endOffset(), "Version to extract", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_GP_BIT =
      new ZipField.F2(F_VERSION_EXTRACT.endOffset(), "GP bit");

  private static final ZipField.F2 F_METHOD = new ZipField.F2(F_GP_BIT.endOffset(), "Method");

  private static final ZipField.F2 F_LAST_MOD_TIME =
      new ZipField.F2(F_METHOD.endOffset(), "Last modification time");

  private static final ZipField.F2 F_LAST_MOD_DATE =
      new ZipField.F2(F_LAST_MOD_TIME.endOffset(), "Last modification date");

  private static final ZipField.F4 F_CRC32 = new ZipField.F4(F_LAST_MOD_DATE.endOffset(), "CRC32");

  private static final ZipField.F4 F_COMPRESSED_SIZE =
      new ZipField.F4(F_CRC32.endOffset(), "Compressed size", new ZipFieldInvariantNonNegative());

  private static final ZipField.F4 F_UNCOMPRESSED_SIZE =
      new ZipField.F4(
          F_COMPRESSED_SIZE.endOffset(), "Uncompressed size", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_FILE_NAME_LENGTH =
      new ZipField.F2(
          F_UNCOMPRESSED_SIZE.endOffset(), "File name length", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_EXTRA_FIELD_LENGTH =
      new ZipField.F2(
          F_FILE_NAME_LENGTH.endOffset(), "Extra field length", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_COMMENT_LENGTH =
      new ZipField.F2(
          F_EXTRA_FIELD_LENGTH.endOffset(), "Comment length", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_DISK_NUMBER_START =
      new ZipField.F2(F_COMMENT_LENGTH.endOffset(), 0, "Disk start");

  private static final ZipField.F2 F_INTERNAL_ATTRIBUTES =
      new ZipField.F2(F_DISK_NUMBER_START.endOffset(), "Int attributes");

  private static final ZipField.F4 F_EXTERNAL_ATTRIBUTES =
      new ZipField.F4(F_INTERNAL_ATTRIBUTES.endOffset(), "Ext attributes");

  private static final ZipField.F4 F_OFFSET =
      new ZipField.F4(
          F_EXTERNAL_ATTRIBUTES.endOffset(), "Offset", new ZipFieldInvariantNonNegative());

  private static final int MAX_VERSION_TO_EXTRACT = 20;

  private static final int ASCII_BIT = 1;

  private final Map<String, StoredEntry> entries;

  private final ZFile file;

  private final CachedSupplier<byte[]> bytesSupplier;

  private final VerifyLog verifyLog;

  CentralDirectory(ZFile file) {
    entries = Maps.newHashMap();
    this.file = file;
    bytesSupplier = new CachedSupplier<>(this::computeByteRepresentation);
    verifyLog = file.getVerifyLog();
  }

  static CentralDirectory makeFromData(ByteBuffer bytes, long count, ZFile file, ByteStorage storage)
      throws IOException {
    Preconditions.checkNotNull(bytes, "bytes == null");
    Preconditions.checkArgument(count >= 0, "count < 0");

    CentralDirectory directory = new CentralDirectory(file);

    for (long i = 0; i < count; i++) {
      try {
        directory.readEntry(bytes, storage);
      } catch (IOException e) {
        throw new IOException(
            "Failed to read directory entry index "
                + i
                + " (total "
                + "directory bytes read: "
                + bytes.position()
                + ").",
            e);
      }
    }

    return directory;
  }

  static CentralDirectory makeFromEntries(Set<StoredEntry> entries, ZFile file) {
    CentralDirectory directory = new CentralDirectory(file);
    for (StoredEntry entry : entries) {
      CentralDirectoryHeader cdr = entry.getCentralDirectoryHeader();
      Preconditions.checkArgument(
          !directory.entries.containsKey(cdr.getName()), "Duplicate filename");
      directory.entries.put(cdr.getName(), entry);
    }

    return directory;
  }

  private void readEntry(ByteBuffer bytes, ByteStorage storage) throws IOException {
    F_SIGNATURE.verify(bytes);
    long madeBy = F_MADE_BY.read(bytes);

    long versionNeededToExtract = F_VERSION_EXTRACT.read(bytes);
    verifyLog.verify(
        versionNeededToExtract <= MAX_VERSION_TO_EXTRACT,
        "Ignored unknown version needed to extract in zip directory entry: %s.",
        versionNeededToExtract);

    long gpBit = F_GP_BIT.read(bytes);
    GPFlags flags = GPFlags.from(gpBit);

    long methodCode = F_METHOD.read(bytes);
    CompressionMethod method = CompressionMethod.fromCode(methodCode);
    verifyLog.verify(method != null, "Unknown method in zip directory entry: %s.", methodCode);

    long lastModTime;
    long lastModDate;
    if (file.areTimestampsIgnored()) {
      lastModTime = 0;
      lastModDate = 0;
      F_LAST_MOD_TIME.skip(bytes);
      F_LAST_MOD_DATE.skip(bytes);
    } else {
      lastModTime = F_LAST_MOD_TIME.read(bytes);
      lastModDate = F_LAST_MOD_DATE.read(bytes);
    }

    long crc32 = F_CRC32.read(bytes);
    long compressedSize = F_COMPRESSED_SIZE.read(bytes);
    long uncompressedSize = F_UNCOMPRESSED_SIZE.read(bytes);
    int fileNameLength = Ints.checkedCast(F_FILE_NAME_LENGTH.read(bytes));
    int extraFieldLength = Ints.checkedCast(F_EXTRA_FIELD_LENGTH.read(bytes));
    int fileCommentLength = Ints.checkedCast(F_COMMENT_LENGTH.read(bytes));

    F_DISK_NUMBER_START.verify(bytes, verifyLog);
    long internalAttributes = F_INTERNAL_ATTRIBUTES.read(bytes);
    verifyLog.verify(
        (internalAttributes & ~ASCII_BIT) == 0,
        "Ignored invalid internal attributes: %s.",
        internalAttributes);

    long externalAttributes = F_EXTERNAL_ATTRIBUTES.read(bytes);
    long entryOffset = F_OFFSET.read(bytes);

    long remainingSize = (long) fileNameLength + extraFieldLength + fileCommentLength;

    if (bytes.remaining() < fileNameLength + extraFieldLength + fileCommentLength) {
      throw new IOException(
          "Directory entry should have "
              + remainingSize
              + " bytes remaining (name = "
              + fileNameLength
              + ", extra = "
              + extraFieldLength
              + ", comment = "
              + fileCommentLength
              + "), but it has "
              + bytes.remaining()
              + ".");
    }

    byte[] encodedFileName = new byte[fileNameLength];
    bytes.get(encodedFileName);
    String fileName = EncodeUtils.decode(encodedFileName, flags);

    byte[] extraField = new byte[extraFieldLength];
    bytes.get(extraField);

    byte[] fileCommentField = new byte[fileCommentLength];
    bytes.get(fileCommentField);

    /*
     * Tricky: to create a CentralDirectoryHeader we need the future that will hold the result
     * of the compress information. But, to actually create the result of the compress
     * information we need the CentralDirectoryHeader
     */
    ListenableFuture<CentralDirectoryHeaderCompressInfo> compressInfo =
        Futures.immediateFuture(
            new CentralDirectoryHeaderCompressInfo(method, compressedSize, versionNeededToExtract));
    CentralDirectoryHeader centralDirectoryHeader =
        new CentralDirectoryHeader(
            fileName,
            encodedFileName,
            uncompressedSize,
            compressInfo,
            flags,
            file,
            lastModTime,
            lastModDate);
    centralDirectoryHeader.setMadeBy(madeBy);
    centralDirectoryHeader.setLastModTime(lastModTime);
    centralDirectoryHeader.setLastModDate(lastModDate);
    centralDirectoryHeader.setCrc32(crc32);
    centralDirectoryHeader.setInternalAttributes(internalAttributes);
    centralDirectoryHeader.setExternalAttributes(externalAttributes);
    centralDirectoryHeader.setOffset(entryOffset);
    centralDirectoryHeader.setExtraFieldNoNotify(new ExtraField(extraField));
    centralDirectoryHeader.setComment(fileCommentField);

    StoredEntry entry;

    try {
      entry = new StoredEntry(centralDirectoryHeader, file, null, storage);
    } catch (IOException e) {
      throw new IOException("Failed to read stored entry '" + fileName + "'.", e);
    }

    if (entries.containsKey(fileName)) {
      verifyLog.log("File file contains duplicate file '" + fileName + "'.");
    }

    entries.put(fileName, entry);
  }

  Map<String, StoredEntry> getEntries() {
    return ImmutableMap.copyOf(entries);
  }

  boolean containsZip64Files() {
    return false;
  }

  byte[] toBytes() throws IOException {
    return bytesSupplier.get();
  }

  private byte[] computeByteRepresentation() {

    List<StoredEntry> sorted = Lists.newArrayList(entries.values());
    Collections.sort(sorted, StoredEntry.COMPARE_BY_NAME);

    CentralDirectoryHeader[] cdhs = new CentralDirectoryHeader[entries.size()];
    CentralDirectoryHeaderCompressInfo[] compressInfos =
        new CentralDirectoryHeaderCompressInfo[entries.size()];
    byte[][] encodedFileNames = new byte[entries.size()][];
    byte[][] extraFields = new byte[entries.size()][];
    byte[][] comments = new byte[entries.size()][];

    try {
      /*
       * First collect all the data and compute the total size of the central directory.
       */
      int idx = 0;
      int total = 0;
      for (StoredEntry entry : sorted) {
        cdhs[idx] = entry.getCentralDirectoryHeader();
        compressInfos[idx] = cdhs[idx].getCompressionInfoWithWait();
        encodedFileNames[idx] = cdhs[idx].getEncodedFileName();
        extraFields[idx] = new byte[cdhs[idx].getExtraField().size()];
        cdhs[idx].getExtraField().write(ByteBuffer.wrap(extraFields[idx]));
        comments[idx] = cdhs[idx].getComment();

        total +=
            F_OFFSET.endOffset()
                + encodedFileNames[idx].length
                + extraFields[idx].length
                + comments[idx].length;
        idx++;
      }

      ByteBuffer out = ByteBuffer.allocate(total);

      for (idx = 0; idx < entries.size(); idx++) {
        F_SIGNATURE.write(out);
        F_MADE_BY.write(out, cdhs[idx].getMadeBy());
        F_VERSION_EXTRACT.write(out, compressInfos[idx].getVersionExtract());
        F_GP_BIT.write(out, cdhs[idx].getGpBit().getValue());
        F_METHOD.write(out, compressInfos[idx].getMethod().methodCode);

        if (file.areTimestampsIgnored()) {
          F_LAST_MOD_TIME.write(out, 0);
          F_LAST_MOD_DATE.write(out, 0);
        } else {
          F_LAST_MOD_TIME.write(out, cdhs[idx].getLastModTime());
          F_LAST_MOD_DATE.write(out, cdhs[idx].getLastModDate());
        }

        F_CRC32.write(out, cdhs[idx].getCrc32());
        F_COMPRESSED_SIZE.write(out, compressInfos[idx].getCompressedSize());
        F_UNCOMPRESSED_SIZE.write(out, cdhs[idx].getUncompressedSize());

        F_FILE_NAME_LENGTH.write(out, cdhs[idx].getEncodedFileName().length);
        F_EXTRA_FIELD_LENGTH.write(out, cdhs[idx].getExtraField().size());
        F_COMMENT_LENGTH.write(out, cdhs[idx].getComment().length);
        F_DISK_NUMBER_START.write(out);
        F_INTERNAL_ATTRIBUTES.write(out, cdhs[idx].getInternalAttributes());
        F_EXTERNAL_ATTRIBUTES.write(out, cdhs[idx].getExternalAttributes());
        F_OFFSET.write(out, cdhs[idx].getOffset());

        out.put(encodedFileNames[idx]);
        out.put(extraFields[idx]);
        out.put(comments[idx]);
      }

      return out.array();
    } catch (IOException e) {
      throw new IOExceptionWrapper(e);
    }
  }
}