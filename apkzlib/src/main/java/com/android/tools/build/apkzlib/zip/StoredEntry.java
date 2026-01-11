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
import com.android.tools.build.apkzlib.bytestorage.CloseableByteSourceFromOutputStreamBuilder;
import com.android.tools.build.apkzlib.utils.IOExceptionWrapper;
import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Verify;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;

import javax.annotation.Nullable;

public class StoredEntry {

  static final Comparator<StoredEntry> COMPARE_BY_NAME =
      (o1, o2) -> {
        if (o1 == null && o2 == null) {
          return 0;
        }

        if (o1 == null) {
          return -1;
        }

        if (o2 == null) {
          return 1;
        }

        String name1 = o1.getCentralDirectoryHeader().getName();
        String name2 = o2.getCentralDirectoryHeader().getName();
        return name1.compareTo(name2);
      };

  private static final int DATA_DESC_SIGNATURE = 0x08074b50;

  private static final ZipField.F4 F_LOCAL_SIGNATURE = new ZipField.F4(0, 0x04034b50, "Signature");

  @VisibleForTesting
  static final ZipField.F2 F_VERSION_EXTRACT =
      new ZipField.F2(
          F_LOCAL_SIGNATURE.endOffset(), "Version to extract", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_GP_BIT =
      new ZipField.F2(F_VERSION_EXTRACT.endOffset(), "GP bit flag");

  private static final ZipField.F2 F_METHOD =
      new ZipField.F2(
          F_GP_BIT.endOffset(), "Compression method", new ZipFieldInvariantNonNegative());

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
          F_UNCOMPRESSED_SIZE.endOffset(), "@File name length", new ZipFieldInvariantNonNegative());

  private static final ZipField.F2 F_EXTRA_LENGTH =
      new ZipField.F2(
          F_FILE_NAME_LENGTH.endOffset(), "Extra length", new ZipFieldInvariantNonNegative());

  static final int FIXED_LOCAL_FILE_HEADER_SIZE = F_EXTRA_LENGTH.endOffset();

  private final StoredEntryType type;

  private final CentralDirectoryHeader cdh;

  private final ZFile file;

  private boolean deleted;

  private ExtraField localExtra;

  private Supplier<DataDescriptorType> dataDescriptorType;

  private ProcessedAndRawByteSources source;

  private final VerifyLog verifyLog;

  private final ByteStorage storage;

  private final StoredEntry linkedEntry;

  private final long nestedOffset;

  private final boolean dummy;

  StoredEntry(
          CentralDirectoryHeader header,
          ZFile file,
          @Nullable ProcessedAndRawByteSources source,
          ByteStorage storage)
          throws IOException {
      this(header, file, source, storage, null, 0, false);
  }

  StoredEntry(
          String name,
          ZFile file,
          ByteStorage storage,
          StoredEntry linkedEntry,
          StoredEntry nestedEntry,
          long nestedOffset,
          boolean dummy)
          throws IOException {
      this((nestedEntry == null ? linkedEntry: nestedEntry).linkingCentralDirectoryHeader(name, file),
              file, (nestedEntry == null ? linkedEntry : nestedEntry).getSource(), storage, linkedEntry, nestedOffset, dummy);
  }

  private CentralDirectoryHeader linkingCentralDirectoryHeader(String name, ZFile file) {
    boolean encodeWithUtf8 = !EncodeUtils.canAsciiEncode(name);
    GPFlags flags = GPFlags.make(encodeWithUtf8);
    return cdh.link(name, EncodeUtils.encode(name, flags), flags, file);
  }

  private StoredEntry(
      CentralDirectoryHeader header,
      ZFile file,
      @Nullable ProcessedAndRawByteSources source,
      ByteStorage storage,
      StoredEntry linkedEntry,
      long nestedOffset,
      boolean dummy)
      throws IOException {
    cdh = header;
    this.file = file;
    deleted = false;
    verifyLog = file.makeVerifyLog();
    this.storage = storage;
    this.linkedEntry = linkedEntry;
    this.nestedOffset = nestedOffset;
    this.dummy = dummy;

    if (header.getOffset() >= 0) {
      readLocalHeader();

      Preconditions.checkArgument(
          source == null, "Source was defined but contents already exist on file.");

      /*
       * Since the file is already in the zip, dynamically create a source that will read
       * the file from the zip when needed. The assignment is not really needed, but we
       * would get a warning because of the @NotNull otherwise.
       */
      this.source = createSourceFromZip(cdh.getOffset());
    } else {
      /*
       * There is no local extra data for new files.
       */
      localExtra = new ExtraField();

      Preconditions.checkNotNull(source, "Source was not defined, but contents are not on file.");
      this.source = source;
    }

    /*
     * It seems that zip utilities store directories as names ending with "/".
     * This seems to be respected by all zip utilities although I could not find there anywhere
     * in the specification.
     */
    if (cdh.getName().endsWith(Character.toString(ZFile.SEPARATOR))) {
      type = StoredEntryType.DIRECTORY;
      verifyLog.verify(
          this.source.getProcessedByteSource().isEmpty(), "Directory source is not empty.");
      verifyLog.verify(cdh.getCrc32() == 0, "Directory has CRC32 = %s.", cdh.getCrc32());
      verifyLog.verify(
          cdh.getUncompressedSize() == 0,
          "Directory has uncompressed size = %s.",
          cdh.getUncompressedSize());

      /*
       * Some clever (OMG!) tools, like jar will actually try to compress the directory
       * contents and generate a 2 byte compressed data. Of course, the uncompressed size is
       * zero and we're just wasting space.
       */
      long compressedSize = cdh.getCompressionInfoWithWait().getCompressedSize();
      verifyLog.verify(
          compressedSize == 0 || compressedSize == 2,
          "Directory has compressed size = %s.",
          compressedSize);
    } else {
      type = StoredEntryType.FILE;
    }

    /*
     * By default we assume there is no data descriptor unless the CRC is marked as deferred
     * in the header's GP Bit.
     */
    dataDescriptorType = Suppliers.ofInstance(DataDescriptorType.NO_DATA_DESCRIPTOR);
    if (header.getGpBit().isDeferredCrc()) {
      /*
       * If the deferred CRC bit exists, then we have an extra descriptor field. This extra
       * field may have a signature.
       */
      Verify.verify(
          header.getOffset() >= 0,
          "Files that are not on disk cannot have the " + "deferred CRC bit set.");

      dataDescriptorType =
          Suppliers.memoize(
              () -> {
                try {
                  return readDataDescriptorRecord();
                } catch (IOException e) {
                  throw new IOExceptionWrapper(
                      new IOException("Failed to read data descriptor record.", e));
                }
              });
    }
  }

  public int getLocalHeaderSize() {
    Preconditions.checkState(!deleted, "deleted");
    return FIXED_LOCAL_FILE_HEADER_SIZE + cdh.getEncodedFileName().length + localExtra.size();
  }

  long getInFileSize() throws IOException {
    Preconditions.checkState(!deleted, "deleted");
    return cdh.getCompressionInfoWithWait().getCompressedSize()
        + getLocalHeaderSize()
        + dataDescriptorType.get().size;
  }

  public InputStream open() throws IOException {
    return source.getProcessedByteSource().openStream();
  }

  public byte[] read() throws IOException {
    try (InputStream is = new BufferedInputStream(open())) {
      return ByteStreams.toByteArray(is);
    }
  }

  public int read(byte[] bytes) throws IOException {
    if (bytes.length < getCentralDirectoryHeader().getUncompressedSize()) {
      throw new RuntimeException(
          "Buffer to small while reading {}" + getCentralDirectoryHeader().getName());
    }
    try (InputStream is = new BufferedInputStream(open())) {
      return ByteStreams.read(is, bytes, 0, bytes.length);
    }
  }

  public StoredEntryType getType() {
    Preconditions.checkState(!deleted, "deleted");
    return type;
  }

  public void delete() throws IOException {
    delete(true);
  }

  void delete(boolean notify) throws IOException {
    Preconditions.checkState(!deleted, "deleted");
    file.delete(this, notify);
    deleted = true;
    source.close();
  }

  public boolean isDeleted() {
    return deleted;
  }

  public CentralDirectoryHeader getCentralDirectoryHeader() {
    return cdh;
  }

  private void readLocalHeader() throws IOException {
    byte[] localHeader = new byte[FIXED_LOCAL_FILE_HEADER_SIZE];
    file.directFullyRead(cdh.getOffset(), localHeader);

    CentralDirectoryHeaderCompressInfo compressInfo = cdh.getCompressionInfoWithWait();

    ByteBuffer bytes = ByteBuffer.wrap(localHeader);
    F_LOCAL_SIGNATURE.verify(bytes);
    F_VERSION_EXTRACT.verify(bytes, compressInfo.getVersionExtract(), verifyLog);
    F_GP_BIT.verify(bytes, cdh.getGpBit().getValue(), verifyLog);
    F_METHOD.verify(bytes, compressInfo.getMethod().methodCode, verifyLog);

    if (file.areTimestampsIgnored()) {
      F_LAST_MOD_TIME.skip(bytes);
      F_LAST_MOD_DATE.skip(bytes);
    } else {
      F_LAST_MOD_TIME.verify(bytes, cdh.getLastModTime(), verifyLog);
      F_LAST_MOD_DATE.verify(bytes, cdh.getLastModDate(), verifyLog);
    }

    /*
     * If CRC-32, compressed size and uncompressed size are deferred, their values in Local
     * File Header must be ignored and their actual values must be read from the Data
     * Descriptor following the contents of this entry. See readDataDescriptorRecord().
     */
    if (cdh.getGpBit().isDeferredCrc()) {
      F_CRC32.skip(bytes);
      F_COMPRESSED_SIZE.skip(bytes);
      F_UNCOMPRESSED_SIZE.skip(bytes);
    } else {
      F_CRC32.verify(bytes, cdh.getCrc32(), verifyLog);
      F_COMPRESSED_SIZE.verify(bytes, compressInfo.getCompressedSize(), verifyLog);
      F_UNCOMPRESSED_SIZE.verify(bytes, cdh.getUncompressedSize(), verifyLog);
    }

    F_FILE_NAME_LENGTH.verify(bytes, cdh.getEncodedFileName().length);
    long extraLength = F_EXTRA_LENGTH.read(bytes);
    long fileNameStart = cdh.getOffset() + F_EXTRA_LENGTH.endOffset();
    byte[] fileNameData = new byte[cdh.getEncodedFileName().length];
    file.directFullyRead(fileNameStart, fileNameData);

    String fileName = EncodeUtils.decode(fileNameData, cdh.getGpBit());
    if (!fileName.equals(cdh.getName())) {
      verifyLog.log(
          String.format(
              "Central directory reports file as being named '%s' but local header"
                  + "reports file being named '%s'.",
              cdh.getName(), fileName));
    }

    long localExtraStart = fileNameStart + cdh.getEncodedFileName().length;
    byte[] localExtraRaw = new byte[Ints.checkedCast(extraLength)];
    file.directFullyRead(localExtraStart, localExtraRaw);
    localExtra = new ExtraField(localExtraRaw);
  }

  private DataDescriptorType readDataDescriptorRecord() throws IOException {
    CentralDirectoryHeaderCompressInfo compressInfo = cdh.getCompressionInfoWithWait();

    long ddStart =
        cdh.getOffset()
            + FIXED_LOCAL_FILE_HEADER_SIZE
            + cdh.getName().length()
            + localExtra.size()
            + compressInfo.getCompressedSize();
    byte[] ddData = new byte[DataDescriptorType.DATA_DESCRIPTOR_WITH_SIGNATURE.size];
    file.directFullyRead(ddStart, ddData);

    ByteBuffer ddBytes = ByteBuffer.wrap(ddData);

    ZipField.F4 signatureField = new ZipField.F4(0, "Data descriptor signature");
    int cpos = ddBytes.position();
    long sig = signatureField.read(ddBytes);
    DataDescriptorType result;
    if (sig == DATA_DESC_SIGNATURE) {
      result = DataDescriptorType.DATA_DESCRIPTOR_WITH_SIGNATURE;
    } else {
      result = DataDescriptorType.DATA_DESCRIPTOR_WITHOUT_SIGNATURE;
      ddBytes.position(cpos);
    }

    ZipField.F4 crc32Field = new ZipField.F4(0, "CRC32");
    ZipField.F4 compressedField = new ZipField.F4(crc32Field.endOffset(), "Compressed size");
    ZipField.F4 uncompressedField =
        new ZipField.F4(compressedField.endOffset(), "Uncompressed size");

    crc32Field.verify(ddBytes, cdh.getCrc32(), verifyLog);
    compressedField.verify(ddBytes, compressInfo.getCompressedSize(), verifyLog);
    uncompressedField.verify(ddBytes, cdh.getUncompressedSize(), verifyLog);
    return result;
  }

  private ProcessedAndRawByteSources createSourceFromZip(final long zipOffset) throws IOException {
    Preconditions.checkArgument(zipOffset >= 0, "zipOffset < 0");

    final CentralDirectoryHeaderCompressInfo compressInfo;
    try {
      compressInfo = cdh.getCompressionInfoWithWait();
    } catch (IOException e) {
      throw new RuntimeException(
          "IOException should never occur here because compression "
              + "information should be immediately available if reading from zip.",
          e);
    }

    /*
     * Create a source that will return whatever is on the zip file.
     */
    CloseableByteSource rawContents =
        new CloseableByteSource() {
          @Override
          public long size() throws IOException {
            return compressInfo.getCompressedSize();
          }

          @Override
          public InputStream openStream() throws IOException {
            Preconditions.checkState(!deleted, "deleted");

            long dataStart = zipOffset + getLocalHeaderSize();
            long dataEnd = dataStart + compressInfo.getCompressedSize();

            file.openReadOnlyIfClosed();
            return file.directOpen(dataStart, dataEnd);
          }

          @Override
          protected void innerClose() throws IOException {
            /*
             * Nothing to do here.
             */
          }
        };

    return createSourcesFromRawContents(rawContents);
  }

  private ProcessedAndRawByteSources createSourcesFromRawContents(CloseableByteSource rawContents) {
    CentralDirectoryHeaderCompressInfo compressInfo;
    try {
      compressInfo = cdh.getCompressionInfoWithWait();
    } catch (IOException e) {
      throw new RuntimeException(
          "IOException should never occur here because compression "
              + "information should be immediately available if creating from raw "
              + "contents.",
          e);
    }

    CloseableByteSource contents;

    /*
     * If the contents are deflated, wrap that source in an inflater source so we get the
     * uncompressed data.
     */
    if (compressInfo.getMethod() == CompressionMethod.DEFLATE) {
      contents = new InflaterByteSource(rawContents);
    } else {
      contents = rawContents;
    }

    return new ProcessedAndRawByteSources(contents, rawContents);
  }

  void replaceSourceFromZip(long zipFileOffset) throws IOException {
    Preconditions.checkArgument(zipFileOffset >= 0, "zipFileOffset < 0");

    ProcessedAndRawByteSources oldSource = source;
    source = createSourceFromZip(zipFileOffset);
    cdh.setOffset(zipFileOffset);
    if (!isLinkingEntry())
      oldSource.close();
  }

  void loadSourceIntoMemory() throws IOException {
    if (cdh.getOffset() == -1) {
      /*
       * No offset in the CDR means data has not been written to disk which, in turn,
       * means data is already loaded into memory.
       */
      return;
    }

    CloseableByteSourceFromOutputStreamBuilder rawBuilder = storage.makeBuilder();
    try (InputStream input = source.getRawByteSource().openStream()) {
      ByteStreams.copy(input, rawBuilder);
    }

    CloseableByteSource newRaw = rawBuilder.build();
    ProcessedAndRawByteSources newSources = createSourcesFromRawContents(newRaw);

    try (ProcessedAndRawByteSources oldSource = source) {
      source = newSources;
      cdh.setOffset(-1);
    }
  }

  ProcessedAndRawByteSources getSource() {
    return source;
  }

  public DataDescriptorType getDataDescriptorType() {
    return dataDescriptorType.get();
  }

  boolean removeDataDescriptor() {
    if (dataDescriptorType.get() == DataDescriptorType.NO_DATA_DESCRIPTOR) {
      return false;
    }

    dataDescriptorType = Suppliers.ofInstance(DataDescriptorType.NO_DATA_DESCRIPTOR);
    cdh.resetDeferredCrc();
    return true;
  }

  int toHeaderData(byte[] buffer) throws IOException {
    Preconditions.checkArgument(
        buffer.length
            >= F_EXTRA_LENGTH.endOffset() + cdh.getEncodedFileName().length + localExtra.size(),
        "Buffer should be at least the header size");

    ByteBuffer out = ByteBuffer.wrap(buffer);
    writeData(out);
    return out.position();
  }

  private void writeData(ByteBuffer out) throws IOException {
    writeData(out, 0);
  }

  void writeData(ByteBuffer out, int extraOffset) throws IOException {
    Preconditions.checkArgument(extraOffset >= 0 , "extraOffset < 0");
    CentralDirectoryHeaderCompressInfo compressInfo = cdh.getCompressionInfoWithWait();

    F_LOCAL_SIGNATURE.write(out);
    F_VERSION_EXTRACT.write(out, compressInfo.getVersionExtract());
    F_GP_BIT.write(out, cdh.getGpBit().getValue());
    F_METHOD.write(out, compressInfo.getMethod().methodCode);

    if (file.areTimestampsIgnored()) {
      F_LAST_MOD_TIME.write(out, 0);
      F_LAST_MOD_DATE.write(out, 0);
    } else {
      F_LAST_MOD_TIME.write(out, cdh.getLastModTime());
      F_LAST_MOD_DATE.write(out, cdh.getLastModDate());
    }

    F_CRC32.write(out, cdh.getCrc32());
    F_COMPRESSED_SIZE.write(out, compressInfo.getCompressedSize());
    F_UNCOMPRESSED_SIZE.write(out, cdh.getUncompressedSize());
    F_FILE_NAME_LENGTH.write(out, cdh.getEncodedFileName().length);
    F_EXTRA_LENGTH.write(out, localExtra.size() + extraOffset + nestedOffset);

    out.put(cdh.getEncodedFileName());
    localExtra.write(out);
  }

  public boolean realign() throws IOException {
    Preconditions.checkState(!deleted, "Entry has been deleted.");

    if (isLinkingEntry()) return true;

    return file.realign(this);
  }

  public boolean isLinkingEntry() {
    return linkedEntry != null;
  }

  public boolean isDummyEntry() {
    return dummy;
  }

  public long getNestedOffset() {
    return nestedOffset;
  }

  public ExtraField getLocalExtra() {
    return localExtra;
  }

  public void setLocalExtra(ExtraField localExtra) throws IOException {
    boolean resized = setLocalExtraNoNotify(localExtra);
    file.localHeaderChanged(this, resized);
  }

  boolean setLocalExtraNoNotify(ExtraField localExtra) throws IOException {
    boolean sizeChanged;

    /*
     * Make sure we load into memory.
     *
     * If we change the size of the local header, the actual start of the file changes
     * according to our in-memory structures so, if we don't read the file now, we won't be
     * able to load it later :)
     *
     * But, even if the size doesn't change, we need to read it force the entry to be
     * rewritten otherwise the changes in the local header aren't written. Of course this case
     * may be optimized with some extra complexity added :)
     */
    loadSourceIntoMemory();

    if (this.localExtra.size() != localExtra.size()) {
      sizeChanged = true;
    } else {
      sizeChanged = false;
    }

    this.localExtra = localExtra;
    return sizeChanged;
  }

  public VerifyLog getVerifyLog() {
    return verifyLog;
  }
}