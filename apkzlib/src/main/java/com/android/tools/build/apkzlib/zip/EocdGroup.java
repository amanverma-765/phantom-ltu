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

import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.android.tools.build.apkzlib.zip.utils.LittleEndianUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.annotation.Nullable;

public class EocdGroup {

  private static final int MIN_EOCD_SIZE = 22;

  private static final int MAX_EOCD_COMMENT_SIZE = 65535;

  private static final int LAST_BYTES_TO_READ = MIN_EOCD_SIZE + MAX_EOCD_COMMENT_SIZE;

  private static final int ZIP64_EOCD_LOCATOR_SIGNATURE = 0x07064b50;

  private static final long EOCD_SIGNATURE = 0x06054b50;

  @Nullable
  private FileUseMapEntry<Eocd> eocdEntry;

  @Nullable
  private FileUseMapEntry<Zip64EocdLocator> eocd64Locator;

  @Nullable
  private FileUseMapEntry<Zip64Eocd> eocd64Entry;

  @Nullable
  private byte[] eocdComment;

  @Nullable
  private Zip64ExtensibleDataSector eocdDataSector;

  private boolean useVersion2Header;

  private final ZFile file;

  private final FileUseMap map;

  private final VerifyLog verifyLog;

  EocdGroup(ZFile file, FileUseMap map) {

    eocd64Entry = null;
    eocd64Locator = null;
    eocdEntry = null;
    eocdComment = new byte[0];
    eocdDataSector = new Zip64ExtensibleDataSector();
    this.file = file;
    this.map = map;
    this.verifyLog = file.getVerifyLog();
    useVersion2Header = false;
  }

  void readRecord(long fileLength) throws IOException {
    /*
     * Read the last part of the zip into memory. If we don't find the EOCD signature by then,
     * the file is corrupt.
     */
    int lastToRead = LAST_BYTES_TO_READ;
    if (lastToRead > fileLength) {
      lastToRead = Ints.checkedCast(fileLength);
    }

    byte[] last = new byte[lastToRead];
    file.directFullyRead(fileLength - lastToRead, last);

    /*
     * Start endIdx at the first possible location where the signature can be located and then
     * move backwards. Because the EOCD must have at least MIN_EOCD size, the first byte of the
     * signature (and first byte of the EOCD) must be located at last.length - MIN_EOCD_SIZE.
     *
     * Because the EOCD signature may exist in the file comment, when we find a signature we
     * will try to read the Eocd. If we fail, we continue searching for the signature. However,
     * we will keep the last exception in case we don't find any signature.
     */
    Eocd eocd = null;
    int foundEocdSignatureIdx = -1;
    IOException errorFindingSignature = null;
    long eocdStart = -1;

    for (int endIdx = last.length - MIN_EOCD_SIZE;
        endIdx >= 0 && foundEocdSignatureIdx == -1;
        endIdx--) {

      ByteBuffer potentialLocator = ByteBuffer.wrap(last, endIdx, 4);
      if (LittleEndianUtils.readUnsigned4Le(potentialLocator) == EOCD_SIGNATURE) {

        /*
         * We found a signature. Try to read the EOCD record.
         */

        foundEocdSignatureIdx = endIdx;
        ByteBuffer eocdBytes =
            ByteBuffer.wrap(last, foundEocdSignatureIdx, last.length - foundEocdSignatureIdx);

        try {
          eocd = new Eocd(eocdBytes);

          eocdStart = fileLength - lastToRead + foundEocdSignatureIdx;

          /*
           * Make sure the EOCD takes the whole file up to the end. Log an error if it
           * doesn't.
           */
          if (eocdStart + eocd.getEocdSize() != fileLength) {
            verifyLog.log(
                "EOCD starts at "
                    + eocdStart
                    + " and has "
                    + eocd.getEocdSize()
                    + " bytes, but file ends at "
                    + fileLength
                    + ".");
          }
        } catch (IOException e) {
          if (errorFindingSignature != null) {
            e.addSuppressed(errorFindingSignature);
          }

          errorFindingSignature = e;
          foundEocdSignatureIdx = -1;
          eocd = null;
        }
      }
    }

    if (foundEocdSignatureIdx == -1) {
      throw new IOException(
          "EOCD signature not found in the last " + lastToRead + " bytes of the file.",
          errorFindingSignature);
    }

    Verify.verify(eocdStart >= 0);
    eocdEntry = map.add(eocdStart, eocdStart + eocd.getEocdSize(), eocd);

    /*
     * Look for the Zip64 central directory locator. If we find it, then this file is a Zip64
     * file and we need to read both the Zip64 EOCD locator and Zip64 EOCD
     */
    long zip64LocatorStart = eocdStart - Zip64EocdLocator.LOCATOR_SIZE;
    if (zip64LocatorStart >= 0) {
      byte[] possibleZip64Locator = new byte[Zip64EocdLocator.LOCATOR_SIZE];
      file.directFullyRead(zip64LocatorStart, possibleZip64Locator);
      if (LittleEndianUtils.readUnsigned4Le(ByteBuffer.wrap(possibleZip64Locator))
          == ZIP64_EOCD_LOCATOR_SIGNATURE) {

        /* found the locator. Read it into memory. */

        Zip64EocdLocator locator = new Zip64EocdLocator(ByteBuffer.wrap(possibleZip64Locator));
        eocd64Locator = map.add(
            zip64LocatorStart, zip64LocatorStart + locator.getSize(), locator);

        /* Find the size of the Zip64 EOCD by reading its size field */
        byte[] zip64EocdSizeHolder = new byte[8];
        file.directFullyRead(
            locator.getZ64EocdOffset() + Zip64Eocd.SIZE_OFFSET, zip64EocdSizeHolder);
        long zip64EocdSize =
            LittleEndianUtils.readUnsigned8Le(ByteBuffer.wrap(zip64EocdSizeHolder))
                + Zip64Eocd.TRUE_SIZE_DIFFERENCE;

        /* read the Zip64 EOCD into memory */

        byte[] zip64EocdBytes = new byte[Ints.checkedCast(zip64EocdSize)];
        file.directFullyRead(locator.getZ64EocdOffset(), zip64EocdBytes);
        Zip64Eocd zip64Eocd = new Zip64Eocd(ByteBuffer.wrap(zip64EocdBytes));
        useVersion2Header =
            zip64Eocd.getVersionToExtract()
                >= CentralDirectoryHeaderCompressInfo.VERSION_WITH_CENTRAL_FILE_ENCRYPTION;

        long zip64EocdEnd = locator.getZ64EocdOffset() + zip64EocdSize;
        if (zip64EocdEnd != zip64LocatorStart) {
          String msg =
              "Zip64 EOCD record is stored in ["
                  + locator.getZ64EocdOffset()
                  + " - "
                  + zip64EocdEnd
                  + "] and EOCD starts at "
                  + zip64LocatorStart
                  + ".";

          /*
           * If there is an empty space between the Zip64 EOCD and the EOCD locator, we proceed
           * logging an error. If the Zip64 EOCD ends after the start of the EOCD locator (and
           * therefore, they overlap), throw an exception.
           */
          if (zip64EocdEnd > zip64LocatorStart) {
            throw new IOException(msg);
          } else {
            verifyLog.log(msg);
          }
        }

        eocd64Entry = map.add(
            locator.getZ64EocdOffset(), zip64EocdEnd, zip64Eocd);
      }
    }

  }

  void computeRecord(
      @Nullable FileUseMapEntry<CentralDirectory> directoryEntry,
      long extraDirectoryOffset) throws IOException {

    long dirStart;
    long dirSize;
    long dirNumEntries;

    if (directoryEntry != null) {
      dirStart = directoryEntry.getStart();
      dirSize = directoryEntry.getSize();
      dirNumEntries = directoryEntry.getStore().getEntries().size();
    } else {
      // if we do not have a directory, then we must leave any required offset.
      dirStart = extraDirectoryOffset;
      dirSize = 0;
      dirNumEntries = 0;
    }

    /*
     * We need a Zip64 EOCD if any value overflows or if Zip64 file extensions are used as stated
     * in the Zip Specification.
     */

    boolean useZip64Eocd =
        dirStart > Eocd.MAX_CD_OFFSET ||
            dirSize > Eocd.MAX_CD_SIZE ||
            dirNumEntries > Eocd.MAX_TOTAL_RECORDS ||
            (directoryEntry != null && directoryEntry.getStore().containsZip64Files());

    /* construct the Zip64 EOCD and locator first, as they come before the standard EOCD */
    if (useZip64Eocd) {
      Verify.verify(eocdDataSector != null);
      Zip64Eocd zip64Eocd =
          new Zip64Eocd(dirNumEntries, dirStart, dirSize, useVersion2Header, eocdDataSector);
      eocdDataSector = null;
      byte[] zip64EocdBytes = zip64Eocd.toBytes();
      long zip64Offset = map.size();
      map.extend(zip64Offset + zip64EocdBytes.length);
      eocd64Entry = map.add(zip64Offset, zip64Offset + zip64EocdBytes.length, zip64Eocd);

      Zip64EocdLocator locator = new Zip64EocdLocator(eocd64Entry.getStart());
      byte[] locatorBytes = locator.toBytes();
      long locatorOffset = map.size();
      map.extend(locatorOffset + locatorBytes.length);
      eocd64Locator = map.add(locatorOffset, locatorOffset + locatorBytes.length, locator);
    }

    /* add the EOCD to the end of the file */

    Verify.verify(eocdComment != null);
    Eocd eocd = new Eocd(
        Math.min(dirNumEntries, Eocd.MAX_TOTAL_RECORDS),
        Math.min(dirStart, Eocd.MAX_CD_OFFSET),
        Math.min(dirSize, Eocd.MAX_CD_SIZE),
        eocdComment);
    eocdComment = null;
    byte[] eocdBytes = eocd.toBytes();
    long eocdOffset = map.size();
    map.extend(eocdOffset + eocdBytes.length);
    eocdEntry = map.add(eocdOffset, eocdOffset + eocdBytes.length, eocd);
  }

  void appendToFile() throws IOException {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    if (eocd64Entry != null) {
      Zip64Eocd zip64Eocd = eocd64Entry.getStore();
      Preconditions.checkNotNull(zip64Eocd);
      Zip64EocdLocator locator = eocd64Locator.getStore();
      Preconditions.checkNotNull(locator);

      file.directWrite(eocd64Entry.getStart(), zip64Eocd.toBytes());
      file.directWrite(eocd64Locator.getStart(), locator.toBytes());
    }

    Eocd eocd = eocdEntry.getStore();
    Preconditions.checkNotNull(eocd, "eocd == null");

    byte[] eocdBytes = eocd.toBytes();
    long eocdOffset = eocdEntry.getStart();

    file.directWrite(eocdOffset, eocdBytes);
  }

  byte[] getEocdBytes() throws IOException {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();
    Preconditions.checkNotNull(eocd, "eocd == null");

    return eocd.toBytes();
  }

  @VisibleForTesting
  @Nullable
  byte[] getEocdLocatorBytes() throws IOException {
    Preconditions.checkNotNull(eocdEntry);

    if (eocd64Locator == null) {
      return null;
    }

    return eocd64Locator.getStore().toBytes();
  }

  @VisibleForTesting
  @Nullable
  byte[] getZ64EocdBytes() throws IOException {
    Preconditions.checkNotNull(eocdEntry);

    if (eocd64Entry == null) {
      return null;
    }

    return eocd64Entry.getStore().toBytes();
  }

  boolean isEmpty() {
    return eocdEntry == null;
  }

  void setUseVersion2Header(boolean useVersion2Header) {
    verifyLog.verify(eocdEntry == null, "eocdEntry != null");

    this.useVersion2Header = useVersion2Header;
  }

  boolean usingVersion2Header() {
    return useVersion2Header;
  }

  void deleteRecord() {
    if (eocdEntry != null) {
      map.remove(eocdEntry);

      Eocd eocd = eocdEntry.getStore();
      Verify.verify(eocd != null);
      eocdComment = eocd.getComment();
      eocdEntry = null;
    }

    if (eocd64Locator != null) {
      Verify.verify(eocd64Entry != null);
      eocdDataSector = eocd64Entry.getStore().getExtraFields();
      map.remove(eocd64Locator);
      map.remove(eocd64Entry);
      eocd64Locator = null;
      eocd64Entry = null;
    } else {
      eocdDataSector = new Zip64ExtensibleDataSector();
    }
  }

  void setEocdComment(byte[] comment) {
    if (comment.length > MAX_EOCD_COMMENT_SIZE) {
      throw new IllegalArgumentException(
          "EOCD comment size ("
              + comment.length
              + ") is larger than the maximum allowed ("
              + MAX_EOCD_COMMENT_SIZE
              + ")");
    }

    // Check if the EOCD signature appears anywhere in the comment we need to check if it
    // is valid.
    for (int i = 0; i < comment.length - MIN_EOCD_SIZE; i++) {
      // Remember: little endian...
      ByteBuffer potentialSignature = ByteBuffer.wrap(comment, i, 4);
      try {
        if (LittleEndianUtils.readUnsigned4Le(potentialSignature) == EOCD_SIGNATURE) {
          // We found a possible EOCD signature at position i. Try to read it.
          ByteBuffer bytes = ByteBuffer.wrap(comment, i, comment.length - i);
          try {
            new Eocd(bytes);
            // If a valid record is found in the comment then this corrupts the Zip file record
            // as we look for the EOCD at the back of the file (where the comment is) first.
            throw new IllegalArgumentException(
                "Position " + i + " of the comment contains a valid EOCD record.");
          } catch (IOException e) {
            // Fine, this is an invalid record. Move along...
          }
        }
      } catch (IOException e) {
        throw new IOExceptionWrapper(e);
      }
    }

    deleteRecord();
    eocdComment = new byte[comment.length];
    System.arraycopy(comment, 0, eocdComment, 0, comment.length);
  }

  long getOffset() {
    if (eocdEntry == null) {
      return -1;
    }
    return getRecordStart();
  }

  byte[] getEocdComment() {
    if (eocdEntry == null) {
      Verify.verify(eocdComment != null);
      byte[] eocdCommentCopy = eocdComment.clone();
      return eocdCommentCopy;
    }

    Eocd eocd = eocdEntry.getStore();
    Verify.verify(eocd != null);
    return eocd.getComment();
  }

  long getDirectorySize() {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();

    if (eocd64Entry != null && eocd.getDirectorySize() == Eocd.MAX_CD_SIZE) {
      return eocd64Entry.getStore().getDirectorySize();
    } else {
      return eocd.getDirectorySize();
    }
  }

  long getDirectoryOffset() {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();

    if (eocd64Entry != null && eocd.getDirectoryOffset() == Eocd.MAX_CD_OFFSET) {
      return eocd64Entry.getStore().getDirectoryOffset();
    } else {
      return eocd.getDirectoryOffset();
    }
  }

  long getTotalDirectoryRecords() {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();
    if (eocd64Entry != null && eocd.getTotalRecords() == Eocd.MAX_TOTAL_RECORDS) {
      return eocd64Entry.getStore().getTotalRecords();
    }

    return eocd.getTotalRecords();
  }

  long getRecordStart() {
    Verify.verify(eocdEntry != null, "eocdEntry == null");
    if (eocd64Entry != null) {
      return eocd64Entry.getStart();
    }
    return eocdEntry.getStart();
  }

  public long getRecordSize() {
    if (eocd64Entry != null) {
      Verify.verify(eocdEntry != null);
      return eocdEntry.getEnd() - eocd64Entry.getStart();
    }
    if (eocdEntry == null) {
      return -1;
    }

    return eocdEntry.getSize();
  }

  @Nullable
  public Zip64ExtensibleDataSector getExtensibleData() {
    Verify.verify(eocdEntry != null);
    if (eocd64Entry != null) {
      return eocd64Entry.getStore().getExtraFields();
    }

    return null;
  }
}