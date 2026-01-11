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

import com.android.apksig.util.DataSource;
import com.android.apksig.util.DataSources;
import com.android.tools.build.apkzlib.bytestorage.ByteStorage;
import com.android.tools.build.apkzlib.utils.CachedFileContents;
import com.android.tools.build.apkzlib.utils.IOExceptionFunction;
import com.android.tools.build.apkzlib.utils.IOExceptionRunnable;
import com.android.tools.build.apkzlib.zip.compress.Zip64NotSupportedException;
import com.android.tools.build.apkzlib.zip.utils.ByteTracker;
import com.android.tools.build.apkzlib.zip.utils.CloseableByteSource;
import com.android.tools.build.apkzlib.zip.utils.CloseableDelegateByteSource;
import com.android.tools.build.apkzlib.zip.utils.LittleEndianUtils;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Closer;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

public class ZFile implements Closeable {

  public static final char SEPARATOR = '/';

  private static final int MIN_EOCD_SIZE = 22;

  private static final int ZIP64_EOCD_LOCATOR_SIZE = 20;

  private static final int MAX_EOCD_COMMENT_SIZE = 65535;

  private static final int LAST_BYTES_TO_READ = MIN_EOCD_SIZE + MAX_EOCD_COMMENT_SIZE;

  private static final int ZIP64_EOCD_LOCATOR_SIGNATURE = 0x07064b50;

  private static final byte[] EOCD_SIGNATURE = new byte[] {0x06, 0x05, 0x4b, 0x50};

  private static final int IO_BUFFER_SIZE = 1024 * 1024;

  private static final int MAXIMUM_EXTENSION_CYCLE_COUNT = 10;

  protected static final int MINIMUM_EXTRA_FIELD_SIZE = ExtraField.AlignmentSegment.MINIMUM_SIZE;

  protected static final int MAX_LOCAL_EXTRA_FIELD_CONTENTS_SIZE = (1 << 15) - 1;

  protected final File file;

  @Nullable private RandomAccessFile raf;

  private final FileUseMap map;

  @Nullable private FileUseMapEntry<Eocd> eocdEntry;

  @Nullable private FileUseMapEntry<CentralDirectory> directoryEntry;

  private final Map<String, FileUseMapEntry<StoredEntry>> entries;

  private final List<StoredEntry> uncompressedEntries;

  private final List<StoredEntry> linkingEntries;

  private ZipFileState state;

  private boolean dirty;

  @Nullable private CachedFileContents<Object> closedControl;

  private final AlignmentRule alignmentRule;

  private final List<ZFileExtension> extensions;

  private final List<IOExceptionRunnable> toRun;

  private boolean isNotifying;

  private long extraDirectoryOffset;

  private boolean noTimestamps;

  private final Compressor compressor;

  private final ByteStorage storage;

  private boolean coverEmptySpaceUsingExtraField;

  private boolean autoSortFiles;

  private final Supplier<VerifyLog> verifyLogFactory;

  private final VerifyLog verifyLog;

  private final boolean skipValidation;

  @Nullable private byte[] eocdComment;

  private boolean readOnly;

  @Deprecated
  public ZFile(File file) throws IOException {
    this(file, new ZFileOptions());
  }

  @Deprecated
  public ZFile(File file, ZFileOptions options) throws IOException {
    this(file, options, false);
  }

  @Deprecated
  public ZFile(File file, ZFileOptions options, boolean readOnly) throws IOException {
    this.file = file;
    map =
        new FileUseMap(
            0, options.getCoverEmptySpaceUsingExtraField() ? MINIMUM_EXTRA_FIELD_SIZE : 0);
    this.readOnly = readOnly;
    dirty = false;
    closedControl = null;
    alignmentRule = options.getAlignmentRule();
    extensions = Lists.newArrayList();
    toRun = Lists.newArrayList();
    noTimestamps = options.getNoTimestamps();
    storage = options.getStorageFactory().create();
    compressor = options.getCompressor();
    coverEmptySpaceUsingExtraField = options.getCoverEmptySpaceUsingExtraField();
    autoSortFiles = options.getAutoSortFiles();
    verifyLogFactory = options.getVerifyLogFactory();
    verifyLog = verifyLogFactory.get();
    skipValidation = options.getSkipValidation();

    /*
     * These two values will be overwritten by openReadOnlyIfClosed() below if the file exists.
     */
    state = ZipFileState.CLOSED;
    raf = null;

    if (file.exists()) {
      openReadOnlyIfClosed();
    } else if (readOnly) {
      throw new IOException("File does not exist but read-only mode requested");
    } else {
      dirty = true;
    }

    entries = Maps.newHashMap();
    uncompressedEntries = Lists.newArrayList();
    linkingEntries = Lists.newArrayList();
    extraDirectoryOffset = 0;

    try {
      if (state != ZipFileState.CLOSED) {
        // TODO: to be removed completely once Zip64 is fully supported
        final long MAX_ENTRY_SIZE = 0xFFFFFFFFL; // 2^32-1
        long rafSize = raf.length();
        if (rafSize > MAX_ENTRY_SIZE) {
          throw new IOException("File exceeds size limit of " + MAX_ENTRY_SIZE + ".");
        }

        map.extend(raf.length());
        readData();
      }

      // If we don't have an EOCD entry, set the comment to empty.
      if (eocdEntry == null) {
        eocdComment = new byte[0];
      }

      // Notify the extensions if the zip file has been open.
      if (state != ZipFileState.CLOSED) {
        notify(ZFileExtension::open);
      }
    } catch (Zip64NotSupportedException e) {
      throw e;
    } catch (IOException e) {
      throw new IOException("Failed to read zip file '" + file.getAbsolutePath() + "'.", e);
    } catch (IllegalStateException | IllegalArgumentException | VerifyException e) {
      throw new RuntimeException(
          "Internal error when trying to read zip file '" + file.getAbsolutePath() + "'.", e);
    }
  }

  @Deprecated
  public void openReadOnly() throws IOException {
    openReadOnlyIfClosed();
  }

  public static ZFile openReadOnly(File file) throws IOException {
    return openReadOnly(file, new ZFileOptions());
  }

  public static ZFile openReadOnly(File file, ZFileOptions options) throws IOException {
    return new ZFile(file, options, true);
  }

  public static ZFile openReadWrite(File file) throws IOException {
    return openReadWrite(file, new ZFileOptions());
  }

  public static ZFile openReadWrite(File file, ZFileOptions options) throws IOException {
    return new ZFile(file, options, false);
  }

  public boolean getSkipValidation() {
    return skipValidation;
  }

  public Set<StoredEntry> entries() {
    Map<String, StoredEntry> entries = Maps.newHashMap();

    for (FileUseMapEntry<StoredEntry> mapEntry : this.entries.values()) {
      StoredEntry entry = mapEntry.getStore();
      Preconditions.checkNotNull(entry, "Entry at %s is null", mapEntry.getStart());
      entries.put(entry.getCentralDirectoryHeader().getName(), entry);
    }

    /*
     * mUncompressed may override mEntriesReady as we may not have yet processed all
     * entries.
     */
    for (StoredEntry uncompressed : uncompressedEntries) {
      entries.put(uncompressed.getCentralDirectoryHeader().getName(), uncompressed);
    }

    for (StoredEntry linking: linkingEntries) {
      entries.put(linking.getCentralDirectoryHeader().getName(), linking);
    }

    return Sets.newHashSet(entries.values());
  }

  @Nullable
  public StoredEntry get(String path) {
    /*
     * The latest entries are the last ones in uncompressed and they may eventually override
     * files in entries.
     */
    for (StoredEntry stillUncompressed : Lists.reverse(uncompressedEntries)) {
      if (stillUncompressed.getCentralDirectoryHeader().getName().equals(path)) {
        return stillUncompressed;
      }
    }

    FileUseMapEntry<StoredEntry> found = entries.get(path);
    if (found == null) {
      return null;
    }

    return found.getStore();
  }

  private void readData() throws IOException {
    Preconditions.checkState(state != ZipFileState.CLOSED, "state == ZipFileState.CLOSED");
    Preconditions.checkNotNull(raf, "raf == null");

    readEocd();
    readCentralDirectory();

    /*
     * Go over all files and create the usage map, verifying there is no overlap in the files.
     */
    long entryEndOffset;
    long directoryStartOffset;

    if (directoryEntry != null) {
      CentralDirectory directory = directoryEntry.getStore();
      Preconditions.checkNotNull(directory, "Central directory is null");

      entryEndOffset = 0;

      for (StoredEntry entry : directory.getEntries().values()) {
        long start = entry.getCentralDirectoryHeader().getOffset();
        long end = start + entry.getInFileSize();

        /*
         * If isExtraAlignmentBlock(entry.getLocalExtra()) is true, we know the entry
         * has an extra field that is solely used for alignment. This means the
         * actual entry could start at start + extra.length and leave space before.
         *
         * But, if we did this here, we would be modifying the zip file and that is
         * weird because we're just opening it for reading.
         *
         * The downside is that we will never reuse that space. Maybe one day ZFile
         * can be clever enough to remove the local extra when we start modifying the zip
         * file.
         */

        Verify.verify(start >= 0, "start < 0");
        Verify.verify(end < map.size(), "end >= map.size()");

        FileUseMapEntry<?> found = map.at(start);
        Verify.verifyNotNull(found);

        // We've got a problem if the found entry is not free or is a free entry but
        // doesn't cover the whole file.
        if (!found.isFree() || found.getEnd() < end) {
          if (found.isFree()) {
            found = map.after(found);
            Verify.verify(found != null && !found.isFree());
          }

          Object foundEntry = found.getStore();
          Verify.verify(foundEntry != null);

          // Obtains a custom description of an entry.
          IOExceptionFunction<StoredEntry, String> describe =
              e ->
                  String.format(
                      "'%s' (offset: %d, size: %d)",
                      e.getCentralDirectoryHeader().getName(),
                      e.getCentralDirectoryHeader().getOffset(),
                      e.getInFileSize());

          String overlappingEntryDescription;
          if (foundEntry instanceof StoredEntry) {
            StoredEntry foundStored = (StoredEntry) foundEntry;
            overlappingEntryDescription = describe.apply(foundStored);
          } else {
            overlappingEntryDescription =
                "Central Directory / EOCD: " + found.getStart() + " - " + found.getEnd();
          }

          throw new IOException(
              "Cannot read entry "
                  + describe.apply(entry)
                  + " because it overlaps with "
                  + overlappingEntryDescription);
        }

        FileUseMapEntry<StoredEntry> mapEntry = map.add(start, end, entry);
        entries.put(entry.getCentralDirectoryHeader().getName(), mapEntry);

        if (end > entryEndOffset) {
          entryEndOffset = end;
        }
      }

      directoryStartOffset = directoryEntry.getStart();
    } else {
      /*
       * No directory means an empty zip file. Use the start of the EOCD to compute
       * an existing offset.
       */
      Verify.verifyNotNull(eocdEntry);
      Preconditions.checkNotNull(eocdEntry, "EOCD is null");
      directoryStartOffset = eocdEntry.getStart();
      entryEndOffset = 0;
    }

    /*
     * Check if there is an extra central directory offset. If there is, save it. Note that
     * we can't call extraDirectoryOffset() because that would mark the file as dirty.
     */
    long extraOffset = directoryStartOffset - entryEndOffset;
    Verify.verify(extraOffset >= 0, "extraOffset (%s) < 0", extraOffset);
    extraDirectoryOffset = extraOffset;
  }

  private void readEocd() throws IOException {
    Preconditions.checkState(state != ZipFileState.CLOSED, "state == ZipFileState.CLOSED");
    Preconditions.checkNotNull(raf, "raf == null");

    /*
     * Read the last part of the zip into memory. If we don't find the EOCD signature by then,
     * the file is corrupt.
     */
    int lastToRead = LAST_BYTES_TO_READ;
    if (lastToRead > raf.length()) {
      lastToRead = Ints.checkedCast(raf.length());
    }

    byte[] last = new byte[lastToRead];
    directFullyRead(raf.length() - lastToRead, last);

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
    int foundEocdSignature = -1;
    IOException errorFindingSignature = null;
    long eocdStart = -1;

    for (int endIdx = last.length - MIN_EOCD_SIZE;
        endIdx >= 0 && foundEocdSignature == -1;
        endIdx--) {
      /*
       * Remember: little endian...
       */
      if (last[endIdx] == EOCD_SIGNATURE[3]
          && last[endIdx + 1] == EOCD_SIGNATURE[2]
          && last[endIdx + 2] == EOCD_SIGNATURE[1]
          && last[endIdx + 3] == EOCD_SIGNATURE[0]) {

        /*
         * We found a signature. Try to read the EOCD record.
         */

        foundEocdSignature = endIdx;
        ByteBuffer eocdBytes =
            ByteBuffer.wrap(last, foundEocdSignature, last.length - foundEocdSignature);

        try {
          eocd = new Eocd(eocdBytes);
          eocdStart = raf.length() - lastToRead + foundEocdSignature;

          /*
           * Make sure the EOCD takes the whole file up to the end. Log an error if it
           * doesn't.
           */
          if (eocdStart + eocd.getEocdSize() != raf.length()) {
            verifyLog.log(
                "EOCD starts at "
                    + eocdStart
                    + " and has "
                    + eocd.getEocdSize()
                    + " bytes, but file ends at "
                    + raf.length()
                    + ".");
          }
        } catch (IOException e) {
          if (errorFindingSignature != null) {
            e.addSuppressed(errorFindingSignature);
          }

          errorFindingSignature = e;
          foundEocdSignature = -1;
          eocd = null;
        }
      }
    }

    if (foundEocdSignature == -1) {
      throw new IOException(
          "EOCD signature not found in the last " + lastToRead + " bytes of the file.",
          errorFindingSignature);
    }

    Verify.verify(eocdStart >= 0);

    /*
     * Look for the Zip64 central directory locator. If we find it, then this file is a Zip64
     * file and we do not support it.
     */
    long zip64LocatorStart = eocdStart - ZIP64_EOCD_LOCATOR_SIZE;
    if (zip64LocatorStart >= 0) {
      byte[] possibleZip64Locator = new byte[4];
      directFullyRead(zip64LocatorStart, possibleZip64Locator);
      if (LittleEndianUtils.readUnsigned4Le(ByteBuffer.wrap(possibleZip64Locator))
          == ZIP64_EOCD_LOCATOR_SIGNATURE) {
        throw new Zip64NotSupportedException(
            "Zip64 EOCD locator found but Zip64 format is not supported.");
      }
    }

    eocdEntry = map.add(eocdStart, eocdStart + eocd.getEocdSize(), eocd);
  }

  private void readCentralDirectory() throws IOException {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");
    Preconditions.checkNotNull(eocdEntry.getStore(), "eocdEntry.getStore() == null");
    Preconditions.checkState(state != ZipFileState.CLOSED, "state == ZipFileState.CLOSED");
    Preconditions.checkNotNull(raf, "raf == null");
    Preconditions.checkState(directoryEntry == null, "directoryEntry != null");

    Eocd eocd = eocdEntry.getStore();

    long dirSize = eocd.getDirectorySize();
    if (dirSize > Integer.MAX_VALUE) {
      throw new IOException("Cannot read central directory with size " + dirSize + ".");
    }

    long centralDirectoryEnd = eocd.getDirectoryOffset() + dirSize;
    if (centralDirectoryEnd != eocdEntry.getStart()) {
      String msg =
          "Central directory is stored in ["
              + eocd.getDirectoryOffset()
              + " - "
              + (centralDirectoryEnd - 1)
              + "] and EOCD starts at "
              + eocdEntry.getStart()
              + ".";

      /*
       * If there is an empty space between the central directory and the EOCD, we proceed
       * logging an error. If the central directory ends after the start of the EOCD (and
       * therefore, they overlap), throw an exception.
       */
      if (centralDirectoryEnd > eocdEntry.getSize()) {
        throw new IOException(msg);
      } else {
        verifyLog.log(msg);
      }
    }

    byte[] directoryData = new byte[Ints.checkedCast(dirSize)];
    directFullyRead(eocd.getDirectoryOffset(), directoryData);

    CentralDirectory directory =
        CentralDirectory.makeFromData(
            ByteBuffer.wrap(directoryData), eocd.getTotalRecords(), this, storage);
    if (eocd.getDirectorySize() > 0) {
      directoryEntry =
          map.add(
              eocd.getDirectoryOffset(),
              eocd.getDirectoryOffset() + eocd.getDirectorySize(),
              directory);
    }
  }

  public InputStream directOpen(final long start, final long end) throws IOException {
    Preconditions.checkState(state != ZipFileState.CLOSED, "state == ZipFileState.CLOSED");
    Preconditions.checkNotNull(raf, "raf == null");
    Preconditions.checkArgument(start >= 0, "start < 0");
    Preconditions.checkArgument(end >= start, "end < start");
    Preconditions.checkArgument(end <= raf.length(), "end > raf.length()");

    return new InputStream() {
      private long mCurr = start;

      @Override
      public int read() throws IOException {
        if (mCurr == end) {
          return -1;
        }

        byte[] b = new byte[1];
        int r = directRead(mCurr, b);
        if (r > 0) {
          mCurr++;
          return b[0];
        } else {
          return -1;
        }
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        Preconditions.checkNotNull(b, "b == null");
        Preconditions.checkArgument(off >= 0, "off < 0");
        Preconditions.checkArgument(off <= b.length, "off > b.length");
        Preconditions.checkArgument(len >= 0, "len < 0");
        Preconditions.checkArgument(off + len <= b.length, "off + len > b.length");

        long availableToRead = end - mCurr;
        long toRead = Math.min(len, availableToRead);

        if (toRead == 0) {
          return -1;
        }

        if (toRead > Integer.MAX_VALUE) {
          throw new IOException("Cannot read " + toRead + " bytes.");
        }

        int r = directRead(mCurr, b, off, Ints.checkedCast(toRead));
        if (r > 0) {
          mCurr += r;
        }

        return r;
      }
    };
  }

  void delete(final StoredEntry entry, boolean notify) throws IOException {
    checkNotInReadOnlyMode();

    String path = entry.getCentralDirectoryHeader().getName();
    FileUseMapEntry<StoredEntry> mapEntry = entries.get(path);
    Preconditions.checkNotNull(mapEntry, "mapEntry == null");
    Preconditions.checkArgument(entry == mapEntry.getStore(), "entry != mapEntry.getStore()");

    dirty = true;

    map.remove(mapEntry);
    entries.remove(path);

    if (notify) {
      notify(ext -> ext.removed(entry));
    }
  }

  private void checkNotInReadOnlyMode() {
    if (readOnly) {
      throw new IllegalStateException("Illegal operation in read only model");
    }
  }

  public void update() throws IOException {
    checkNotInReadOnlyMode();

    /*
     * Process all background stuff before calling in the extensions.
     */
    processAllReadyEntriesWithWait();
    notify(ZFileExtension::beforeUpdate);

    /*
     * Process all background stuff that may be leftover by the extensions.
     */
    processAllReadyEntriesWithWait();

    if (dirty) {
      writeAllFilesToZip();
    }

    // Even if no files were modified, we still need to recompute the central directory and EOCD
    // in case they have been modified by any extension.
    recomputeAndWriteCentralDirectoryAndEocd();

    // If there are no changes to the file, we may get here without even opening the zip as a
    // RandomAccessFile. In that case, don't try to change the size since we're sure there are no
    // changes.
    if (raf != null) {
      // Ensure we make the zip have the right size (only useful if shrinking), mark the zip as
      // no longer dirty and notify all extensions.
      if (raf.length() != map.size()) {
        raf.setLength(map.size());
      }
    }

    // Regardless of whether the zip was dirty or not, we're sure it isn't now.
    dirty = false;

    notify(
        ext -> {
          ext.updated();
          return null;
        });
  }

  private void writeAllFilesToZip() throws IOException {
    reopenRw();

    /*
     * At this point, no more files can be added. We may need to repack to remove extra
     * empty spaces or sort. If we sort, we don't need to repack as sorting forces the
     * zip file to be as compact as possible.
     */
    if (autoSortFiles) {
      sortZipContents();
    } else {
      packIfNecessary();
    }

    /*
     * We're going to change the file so delete the central directory and the EOCD as they
     * will have to be rewritten.
     */
    deleteDirectoryAndEocd();
    map.truncate();

    /*
     * If we need to use the extra field to cover empty spaces, we do the processing here.
     */
    if (coverEmptySpaceUsingExtraField) {

      /* We will go over all files in the zip and check whether there is empty space before
       * them. If there is, then we will move the entry to the beginning of the empty space
       * (covering it) and extend the extra field with the size of the empty space.
       */
      for (FileUseMapEntry<StoredEntry> entry : new HashSet<>(entries.values())) {
        StoredEntry storedEntry = entry.getStore();
        Preconditions.checkNotNull(storedEntry, "Entry at %s is null", entry.getStart());

        FileUseMapEntry<?> before = map.before(entry);
        if (before == null || !before.isFree()) {
          continue;
        }

        /*
         * We have free space before the current entry. However, we do know that it can
         * be covered by the extra field, because both sortZipContents() and
         * packIfNecessary() guarantee it.
         */
        int localExtraSize =
            storedEntry.getLocalExtra().size() + Ints.checkedCast(before.getSize());
        Verify.verify(localExtraSize <= MAX_LOCAL_EXTRA_FIELD_CONTENTS_SIZE);

        /*
         * Move file back in the zip.
         */
        storedEntry.loadSourceIntoMemory();

        long newStart = before.getStart();
        long newSize = entry.getSize() + before.getSize();

        /*
         * Remove the entry.
         */
        String name = storedEntry.getCentralDirectoryHeader().getName();
        map.remove(entry);
        Verify.verify(entry == entries.remove(name));

        /*
         * Make a list will all existing segments in the entry's extra field, but remove
         * the alignment field, if it exists. Also, sum the size of all kept extra field
         * segments.
         */
        ImmutableList<ExtraField.Segment> currentSegments;
        try {
          currentSegments = storedEntry.getLocalExtra().getSegments();
        } catch (IOException e) {
          /*
           * Parsing current segments has failed. This means the contents of the extra
           * field are not valid. We'll continue discarding the existing segments.
           */
          currentSegments = ImmutableList.of();
        }

        List<ExtraField.Segment> extraFieldSegments = new ArrayList<>();
        int newExtraFieldSize = 0;
        for (ExtraField.Segment segment : currentSegments) {
          if (segment.getHeaderId() != ExtraField.ALIGNMENT_ZIP_EXTRA_DATA_FIELD_HEADER_ID) {
            extraFieldSegments.add(segment);
            newExtraFieldSize += segment.size();
          }
        }

        int spaceToFill =
            Ints.checkedCast(
                before.getSize() + storedEntry.getLocalExtra().size() - newExtraFieldSize);

        extraFieldSegments.add(
            new ExtraField.AlignmentSegment(chooseAlignment(storedEntry), spaceToFill));

        storedEntry.setLocalExtraNoNotify(new ExtraField(ImmutableList.copyOf(extraFieldSegments)));
        entries.put(name, map.add(newStart, newStart + newSize, storedEntry));

        /*
         * Reset the offset to force the file to be rewritten.
         */
        storedEntry.getCentralDirectoryHeader().setOffset(-1);
      }
    }

    /*
     * Write new files in the zip. We identify new files because they don't have an offset
     * in the zip where they are written although we already know, by their location in the
     * file map, where they will be written to.
     *
     * Before writing the files, we sort them in the order they are written in the file so that
     * writes are made in order on disk.
     * This is, however, unlikely to optimize anything relevant given the way the Operating
     * System does caching, but it certainly won't hurt :)
     */
    TreeMap<FileUseMapEntry<?>, StoredEntry> toWriteToStore =
        new TreeMap<>(FileUseMapEntry.COMPARE_BY_START);

    for (FileUseMapEntry<StoredEntry> entry : entries.values()) {
      StoredEntry entryStore = entry.getStore();
      Preconditions.checkNotNull(entryStore, "Entry at %s is null", entry.getStart());
      if (entryStore.getCentralDirectoryHeader().getOffset() == -1) {
        toWriteToStore.put(entry, entryStore);
      }
    }

    /*
     * Add all free entries to the set.
     */
    for (FileUseMapEntry<?> freeArea : map.getFreeAreas()) {
      toWriteToStore.put(freeArea, null);
    }

    /*
     * Write everything to file.
     */
    byte[] chunk = new byte[IO_BUFFER_SIZE];
    for (FileUseMapEntry<?> fileUseMapEntry : toWriteToStore.keySet()) {
      StoredEntry entry = toWriteToStore.get(fileUseMapEntry);
      if (entry == null) {
        int size = Ints.checkedCast(fileUseMapEntry.getSize());
        directWrite(fileUseMapEntry.getStart(), new byte[size]);
      } else {
        writeEntry(entry, fileUseMapEntry.getStart(), chunk);
      }
    }
  }

  private void recomputeAndWriteCentralDirectoryAndEocd() throws IOException {
    boolean changedAnything = false;
    boolean hasCentralDirectory;
    int extensionBugDetector = MAXIMUM_EXTENSION_CYCLE_COUNT;
    do {
      // Try to compute the central directory and EOCD. Computing the central directory may end
      // with directoryEntry == null if there are no entries in the zip.
      if (directoryEntry == null) {
        reopenRw();
        changedAnything = true;
        computeCentralDirectory();
      }

      if (eocdEntry == null) {
        // It is fine to call computeEocd even if directoryEntry == null as long as the zip has
        // no files.
        reopenRw();
        changedAnything = true;
        computeEocd();
      }

      hasCentralDirectory = (directoryEntry != null);

      notify(
          ext -> {
            ext.entriesWritten();
            return null;
          });

      if ((--extensionBugDetector) == 0) {
        throw new IOException(
            "Extensions keep resetting the central directory. This is " + "probably a bug.");
      }
    } while ((hasCentralDirectory && directoryEntry == null) || eocdEntry == null);

    if (changedAnything) {
      reopenRw();
      appendCentralDirectory();
      appendEocd();
    }
  }

  private void packIfNecessary() throws IOException {
    if (!coverEmptySpaceUsingExtraField) {
      return;
    }

    SortedSet<FileUseMapEntry<StoredEntry>> entriesByLocation =
        new TreeSet<>(FileUseMapEntry.COMPARE_BY_START);
    entriesByLocation.addAll(entries.values());

    for (FileUseMapEntry<StoredEntry> entry : entriesByLocation) {
      StoredEntry storedEntry = entry.getStore();
      Preconditions.checkNotNull(storedEntry, "Entry at %s is null", entry.getStart());

      FileUseMapEntry<?> before = map.before(entry);
      if (before == null || !before.isFree()) {
        continue;
      }

      int localExtraSize = storedEntry.getLocalExtra().size() + Ints.checkedCast(before.getSize());
      if (localExtraSize > MAX_LOCAL_EXTRA_FIELD_CONTENTS_SIZE) {
        /*
         * This entry is too far from the previous one. Remove it and re-add it to the
         * zip file.
         */
        reAdd(storedEntry, PositionHint.LOWEST_OFFSET);
      }
    }
  }

  private void reAdd(StoredEntry entry, PositionHint positionHint) throws IOException {
    String name = entry.getCentralDirectoryHeader().getName();
    FileUseMapEntry<StoredEntry> mapEntry = entries.get(name);
    Preconditions.checkNotNull(mapEntry);
    Preconditions.checkState(mapEntry.getStore() == entry);

    entry.loadSourceIntoMemory();

    map.remove(mapEntry);
    entries.remove(name);
    FileUseMapEntry<StoredEntry> positioned = positionInFile(entry, positionHint);
    entries.put(name, positioned);
    dirty = true;
  }

  void localHeaderChanged(StoredEntry entry, boolean resized) throws IOException {
    dirty = true;

    if (resized) {
      reAdd(entry, PositionHint.ANYWHERE);
    }
  }

  void centralDirectoryChanged() {
    dirty = true;
    deleteDirectoryAndEocd();
  }

  @Override
  public void close() throws IOException {
    // We need to make sure to release raf, otherwise we end up locking the file on
    // Windows. Use try-with-resources to handle exception suppressing.
    try (Closeable ignored = this::innerClose) {
      if (!readOnly) {
        update();
      }

      storage.close();
    }

    notify(
        ext -> {
          ext.closed();
          return null;
        });
  }

  private void deleteDirectoryAndEocd() {
    if (directoryEntry != null) {
      map.remove(directoryEntry);
      directoryEntry = null;
    }

    if (eocdEntry != null) {
      map.remove(eocdEntry);

      Eocd eocd = eocdEntry.getStore();
      Verify.verify(eocd != null);
      eocdComment = eocd.getComment();
      eocdEntry = null;
    }
  }

  private void writeEntry(StoredEntry entry, long offset, byte[] chunk) throws IOException {
    Preconditions.checkArgument(
        entry.getDataDescriptorType() == DataDescriptorType.NO_DATA_DESCRIPTOR,
        "Cannot write entries with a data " + "descriptor.");
    Preconditions.checkNotNull(raf, "raf == null");
    Preconditions.checkState(state == ZipFileState.OPEN_RW, "state != ZipFileState.OPEN_RW");

    int r;
    // Put header data to the beginning of buffer
    // LSPatch: write extra entries in the extra field if it's a linking
    int localHeaderSize = entry.getLocalHeaderSize();
    for (var segment : entry.getLocalExtra().getSegments()) {
      if (segment instanceof ExtraField.LinkingEntrySegment) {
        ((ExtraField.LinkingEntrySegment) segment).setOffset(localHeaderSize, offset);
      }
    }
    int readOffset = entry.toHeaderData(chunk);
    assert localHeaderSize == readOffset;
    long writeOffset = offset;
    try (InputStream is = entry.getSource().getRawByteSource().openStream()) {
      while ((r = is.read(chunk, readOffset, chunk.length - readOffset)) >= 0 || readOffset > 0) {
        int toWrite = (r == -1 ? 0 : r) + readOffset;
        directWrite(writeOffset, chunk, 0, toWrite);
        writeOffset += toWrite;
        readOffset = 0;
      }
    }

    /*
     * Set the entry's offset and create the entry source.
     */
    entry.replaceSourceFromZip(offset);
  }

  private void computeCentralDirectory() throws IOException {
    Preconditions.checkState(state == ZipFileState.OPEN_RW, "state != ZipFileState.OPEN_RW");
    Preconditions.checkNotNull(raf, "raf == null");
    Preconditions.checkState(directoryEntry == null, "directoryEntry != null");

    Set<StoredEntry> newStored = Sets.newHashSet();
    for (FileUseMapEntry<StoredEntry> mapEntry : entries.values()) {
      newStored.add(mapEntry.getStore());
    }

    newStored.addAll(linkingEntries);

    /*
     * Make sure we truncate the map before computing the central directory's location since
     * the central directory is the last part of the file.
     */
    map.truncate();

    CentralDirectory newDirectory = CentralDirectory.makeFromEntries(newStored, this);
    byte[] newDirectoryBytes = newDirectory.toBytes();
    long directoryOffset = map.size() + extraDirectoryOffset;

    map.extend(directoryOffset + newDirectoryBytes.length);

    if (newDirectoryBytes.length > 0) {
      directoryEntry =
          map.add(directoryOffset, directoryOffset + newDirectoryBytes.length, newDirectory);
    }
  }

  private void appendCentralDirectory() throws IOException {
    Preconditions.checkState(state == ZipFileState.OPEN_RW, "state != ZipFileState.OPEN_RW");
    Preconditions.checkNotNull(raf, "raf == null");

    if (entries.isEmpty()) {
      Preconditions.checkState(directoryEntry == null, "directoryEntry != null");
      return;
    }

    Preconditions.checkNotNull(directoryEntry, "directoryEntry != null");

    CentralDirectory newDirectory = directoryEntry.getStore();
    Preconditions.checkNotNull(newDirectory, "newDirectory != null");

    byte[] newDirectoryBytes = newDirectory.toBytes();
    long directoryOffset = directoryEntry.getStart();

    /*
     * It is fine to seek beyond the end of file. Seeking beyond the end of file will not extend
     * the file. Even if we do not have any directory data to write, the extend() call below
     * will force the file to be extended leaving exactly extraDirectoryOffset bytes empty at
     * the beginning.
     */
    directWrite(directoryOffset, newDirectoryBytes);
  }

  public byte[] getCentralDirectoryBytes() throws IOException {
    if (entries.isEmpty()) {
      Preconditions.checkState(directoryEntry == null, "directoryEntry != null");
      return new byte[0];
    }

    Preconditions.checkNotNull(directoryEntry, "directoryEntry == null");

    CentralDirectory cd = directoryEntry.getStore();
    Preconditions.checkNotNull(cd, "cd == null");
    return cd.toBytes();
  }

  private void computeEocd() throws IOException {
    Preconditions.checkState(state == ZipFileState.OPEN_RW, "state != ZipFileState.OPEN_RW");
    Preconditions.checkNotNull(raf, "raf == null");
    if (directoryEntry == null) {
      Preconditions.checkState(entries.isEmpty(), "directoryEntry == null && !entries.isEmpty()");
    }

    long dirStart;
    long dirSize = 0;

    if (directoryEntry != null) {
      CentralDirectory directory = directoryEntry.getStore();

      Preconditions.checkNotNull(directory, "Central directory is null");

      dirStart = directoryEntry.getStart();
      dirSize = directoryEntry.getSize();
      Verify.verify(directory.getEntries().size() == entries.size() + linkingEntries.size());
    } else {
      /*
       * If we do not have a directory, then we must leave any requested offset empty.
       */
      dirStart = extraDirectoryOffset;
    }

    Verify.verify(eocdComment != null);
    Eocd eocd = new Eocd(entries.size() + linkingEntries.size(), dirStart, dirSize, eocdComment);
    eocdComment = null;

    byte[] eocdBytes = eocd.toBytes();
    long eocdOffset = map.size();

    map.extend(eocdOffset + eocdBytes.length);

    eocdEntry = map.add(eocdOffset, eocdOffset + eocdBytes.length, eocd);
  }

  private void appendEocd() throws IOException {
    Preconditions.checkState(state == ZipFileState.OPEN_RW, "state != ZipFileState.OPEN_RW");
    Preconditions.checkNotNull(raf, "raf == null");
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();
    Preconditions.checkNotNull(eocd, "eocd == null");

    byte[] eocdBytes = eocd.toBytes();
    long eocdOffset = eocdEntry.getStart();

    directWrite(eocdOffset, eocdBytes);
  }

  public byte[] getEocdBytes() throws IOException {
    Preconditions.checkNotNull(eocdEntry, "eocdEntry == null");

    Eocd eocd = eocdEntry.getStore();
    Preconditions.checkNotNull(eocd, "eocd == null");
    return eocd.toBytes();
  }

  private void innerClose() throws IOException {
    if (state == ZipFileState.CLOSED) {
      return;
    }

    Verify.verifyNotNull(raf, "raf == null");

    raf.close();
    raf = null;
    state = ZipFileState.CLOSED;
    if (closedControl == null) {
      closedControl = new CachedFileContents<>(file);
    }

    closedControl.closed(null);
  }

  public void openReadOnlyIfClosed() throws IOException {
    if (state != ZipFileState.CLOSED) {
      return;
    }

    state = ZipFileState.OPEN_RO;
    raf = new RandomAccessFile(file, "r");
  }

  private void reopenRw() throws IOException {
    // We an never open a file RW in read-only mode. We should never get this far, though.
    Verify.verify(!readOnly);

    if (state == ZipFileState.OPEN_RW) {
      return;
    }

    boolean wasClosed;
    if (state == ZipFileState.OPEN_RO) {
      /*
       * ReadAccessFile does not have a way to reopen as RW so we have to close it and
       * open it again.
       */
      innerClose();
      wasClosed = false;
    } else {
      wasClosed = true;
    }

    Verify.verify(state == ZipFileState.CLOSED, "state != ZpiFileState.CLOSED");
    Verify.verify(raf == null, "raf != null");

    if (closedControl != null && !closedControl.isValid()) {
      throw new IOException(
          "File '"
              + file.getAbsolutePath()
              + "' has been modified "
              + "by an external application.");
    }

    raf = new RandomAccessFile(file, "rw");
    state = ZipFileState.OPEN_RW;

    /*
     * Now that we've open the zip and are ready to write, clear out any data descriptors
     * in the zip since we don't need them and they take space in the archive.
     */
    for (StoredEntry entry : entries()) {
      dirty |= entry.removeDataDescriptor();
    }

    if (wasClosed) {
      notify(ZFileExtension::open);
    }
  }

  public void add(String name, InputStream stream) throws IOException {
    checkNotInReadOnlyMode();
    add(name, stream, true);
  }

  public StoredEntry add(String name, InputStream stream, boolean mayCompress) throws IOException {
    return add(name, storage.fromStream(stream), mayCompress);
  }

  public void add(String name, ByteSource source, boolean mayCompress) throws IOException {
    Optional<Long> sizeBytes = source.sizeIfKnown();
    if (!sizeBytes.isPresent()) {
      throw new IllegalArgumentException("Can only add ByteSources with known size");
    }
    add(name, new CloseableDelegateByteSource(source, sizeBytes.get()), mayCompress);
  }

  private StoredEntry add(String name, CloseableByteSource source, boolean mayCompress)
      throws IOException {
    checkNotInReadOnlyMode();

    /*
     * Clean pending background work, if needed.
     */
    processAllReadyEntries();

    return add(makeStoredEntry(name, source, mayCompress));
  }

  public void addLink(StoredEntry linkedEntry, String dstName)
          throws IOException {
      addNestedLink(linkedEntry, dstName, null, 0L, false);
  }

  void addNestedLink(StoredEntry linkedEntry, String dstName, StoredEntry nestedEntry, long nestedOffset, boolean dummy)
          throws IOException {
    Preconditions.checkArgument(linkedEntry != null, "linkedEntry is null");
    Preconditions.checkArgument(linkedEntry.getCentralDirectoryHeader().getOffset() < 0, "linkedEntry is not new file");
    Preconditions.checkArgument(!linkedEntry.isLinkingEntry(), "linkedEntry is a linking entry");
    var linkingEntry = new StoredEntry(dstName, this, storage, linkedEntry, nestedEntry, nestedOffset, dummy);
    linkingEntries.add(linkingEntry);
    linkedEntry.setLocalExtraNoNotify(new ExtraField(ImmutableList.<ExtraField.Segment>builder().add(linkedEntry.getLocalExtra().getSegments().toArray(new ExtraField.Segment[0])).add(new ExtraField.LinkingEntrySegment(linkingEntry)).build()));
    reAdd(linkedEntry, PositionHint.LOWEST_OFFSET);
  }

  public NestedZip addNestedZip(NestedZip.NameCallback name, File src, boolean mayCompress) throws IOException {
    return new NestedZip(name, this, src, mayCompress);
  }


  private StoredEntry add(final StoredEntry newEntry) throws IOException {
    uncompressedEntries.add(newEntry);
    processAllReadyEntries();
    return newEntry;
  }

  private StoredEntry makeStoredEntry(String name, CloseableByteSource source, boolean mayCompress)
      throws IOException {
    long crc32 = source.hash(Hashing.crc32()).padToLong();

    boolean encodeWithUtf8 = !EncodeUtils.canAsciiEncode(name);

    SettableFuture<CentralDirectoryHeaderCompressInfo> compressInfo = SettableFuture.create();
    GPFlags flags = GPFlags.make(encodeWithUtf8);
    CentralDirectoryHeader newFileData =
        new CentralDirectoryHeader(
            name, EncodeUtils.encode(name, flags), source.size(), compressInfo, flags, this);
    newFileData.setCrc32(crc32);

    /*
     * Create the new entry and sets its data source. Offset should be set to -1 automatically
     * because this is a new file. With offset set to -1, StoredEntry does not try to verify the
     * local header. Since this is a new file, there is no local header and not checking it is
     * what we want to happen.
     */
    Verify.verify(newFileData.getOffset() == -1);
    return new StoredEntry(
        newFileData, this, createSources(mayCompress, source, compressInfo, newFileData), storage);
  }

  private ProcessedAndRawByteSources createSources(
      boolean mayCompress,
      CloseableByteSource source,
      SettableFuture<CentralDirectoryHeaderCompressInfo> compressInfo,
      CentralDirectoryHeader newFileData)
      throws IOException {
    if (mayCompress) {
      ListenableFuture<CompressionResult> result = compressor.compress(source, storage);
      Futures.addCallback(
          result,
          new FutureCallback<CompressionResult>() {
            @Override
            public void onSuccess(CompressionResult result) {
              compressInfo.set(
                  new CentralDirectoryHeaderCompressInfo(
                      newFileData, result.getCompressionMethod(), result.getSize()));
            }

            @Override
            public void onFailure(Throwable t) {
              compressInfo.setException(t);
            }
          },
          MoreExecutors.directExecutor());

      ListenableFuture<CloseableByteSource> compressedByteSourceFuture =
          Futures.transform(result, CompressionResult::getSource, MoreExecutors.directExecutor());
      LazyDelegateByteSource compressedByteSource =
          new LazyDelegateByteSource(compressedByteSourceFuture);
      return new ProcessedAndRawByteSources(source, compressedByteSource);
    } else {
      compressInfo.set(
          new CentralDirectoryHeaderCompressInfo(
              newFileData, CompressionMethod.STORE, source.size()));
      return new ProcessedAndRawByteSources(source, source);
    }
  }

  private void processAllReadyEntries() throws IOException {
    /*
     * Many things can happen during addToEntries(). Because addToEntries() fires
     * notifications to extensions, other files can be added, removed, etc. Ee are *not*
     * guaranteed that new stuff does not get into uncompressedEntries: add() will still work
     * and will add new entries in there.
     *
     * However -- important -- processReadyEntries() may be invoked during addToEntries()
     * because of the extension mechanism. This means that stuff *can* be removed from
     * uncompressedEntries and moved to entries during addToEntries().
     */
    while (!uncompressedEntries.isEmpty()) {
      StoredEntry next = uncompressedEntries.get(0);
      CentralDirectoryHeader cdh = next.getCentralDirectoryHeader();
      Future<CentralDirectoryHeaderCompressInfo> compressionInfo = cdh.getCompressionInfo();
      if (!compressionInfo.isDone()) {
        /*
         * First entry in queue is not yet complete. We can't do anything else.
         */
        return;
      }

      uncompressedEntries.remove(0);

      try {
        compressionInfo.get();
      } catch (InterruptedException e) {
        throw new IOException(
            "Impossible I/O exception: get for already computed "
                + "future throws InterruptedException",
            e);
      } catch (ExecutionException e) {
        throw new IOException("Failed to obtain compression information for entry", e);
      }

      addToEntries(next);
    }
  }

  private void processAllReadyEntriesWithWait() throws IOException {
    processAllReadyEntries();
    while (!uncompressedEntries.isEmpty()) {
      /*
       * Wait for the first future to complete and then try again. Keep looping until we're
       * done.
       */
      StoredEntry first = uncompressedEntries.get(0);
      CentralDirectoryHeader cdh = first.getCentralDirectoryHeader();
      cdh.getCompressionInfoWithWait();

      processAllReadyEntries();
    }
  }

  private void addToEntries(final StoredEntry newEntry) throws IOException {
    Preconditions.checkArgument(
        newEntry.getDataDescriptorType() == DataDescriptorType.NO_DATA_DESCRIPTOR,
        "newEntry has data descriptor");

    /*
     * If there is a file with the same name in the archive, remove it. We remove it by
     * calling delete() on the entry (this is the public API to remove a file from the archive).
     * StoredEntry.delete() will call {@link ZFile#delete(StoredEntry, boolean)}  to perform
     * data structure cleanup.
     */
    FileUseMapEntry<StoredEntry> toReplace =
        entries.get(newEntry.getCentralDirectoryHeader().getName());
    final StoredEntry replaceStore;
    if (toReplace != null) {
      replaceStore = toReplace.getStore();
      Preconditions.checkNotNull(
          replaceStore, "File to replace at %s is null", toReplace.getStart());
      replaceStore.delete(false);
    } else {
      replaceStore = null;
    }

    FileUseMapEntry<StoredEntry> fileUseMapEntry = positionInFile(newEntry, PositionHint.ANYWHERE);
    entries.put(newEntry.getCentralDirectoryHeader().getName(), fileUseMapEntry);

    dirty = true;

    notify(ext -> ext.added(newEntry, replaceStore));
  }

  private FileUseMapEntry<StoredEntry> positionInFile(StoredEntry entry, PositionHint positionHint)
      throws IOException {
    deleteDirectoryAndEocd();
    long size = entry.getInFileSize();
    int localHeaderSize = entry.getLocalHeaderSize();
    int alignment = chooseAlignment(entry);

    FileUseMap.PositionAlgorithm algorithm;

    switch (positionHint) {
      case LOWEST_OFFSET:
        algorithm = FileUseMap.PositionAlgorithm.FIRST_FIT;
        break;
      case ANYWHERE:
        algorithm = FileUseMap.PositionAlgorithm.BEST_FIT;
        break;
      default:
        throw new AssertionError();
    }

    long newOffset = map.locateFree(size, localHeaderSize, alignment, algorithm);
    long newEnd = newOffset + entry.getInFileSize();
    if (newEnd > map.size()) {
      map.extend(newEnd);
    }

    return map.add(newOffset, newEnd, entry);
  }

  private int chooseAlignment(StoredEntry entry) throws IOException {
    CentralDirectoryHeader cdh = entry.getCentralDirectoryHeader();
    CentralDirectoryHeaderCompressInfo compressionInfo = cdh.getCompressionInfoWithWait();

    boolean isCompressed = compressionInfo.getMethod() != CompressionMethod.STORE;
    if (isCompressed) {
      return AlignmentRule.NO_ALIGNMENT;
    } else {
      return alignmentRule.alignment(cdh.getName());
    }
  }

  public void mergeFrom(ZFile src, Predicate<String> ignoreFilter) throws IOException {
    checkNotInReadOnlyMode();

    for (StoredEntry fromEntry : src.entries()) {
      if (ignoreFilter.apply(fromEntry.getCentralDirectoryHeader().getName())) {
        continue;
      }

      boolean replaceCurrent = true;
      String path = fromEntry.getCentralDirectoryHeader().getName();
      FileUseMapEntry<StoredEntry> currentEntry = entries.get(path);

      if (currentEntry != null) {
        long fromSize = fromEntry.getCentralDirectoryHeader().getUncompressedSize();
        long fromCrc = fromEntry.getCentralDirectoryHeader().getCrc32();

        StoredEntry currentStore = currentEntry.getStore();
        Preconditions.checkNotNull(currentStore, "Entry at %s is null", currentEntry.getStart());

        long currentSize = currentStore.getCentralDirectoryHeader().getUncompressedSize();
        long currentCrc = currentStore.getCentralDirectoryHeader().getCrc32();

        if (fromSize == currentSize && fromCrc == currentCrc) {
          replaceCurrent = false;
        }
      }

      if (replaceCurrent) {
        CentralDirectoryHeader fromCdr = fromEntry.getCentralDirectoryHeader();
        CentralDirectoryHeaderCompressInfo fromCompressInfo = fromCdr.getCompressionInfoWithWait();
        CentralDirectoryHeader newFileData;
        try {
          /*
           * We make two changes in the central directory from the file to merge:
           * we reset the offset to force the entry to be written and we reset the
           * deferred CRC bit as we don't need the extra stuff after the file. It takes
           * space and is totally useless.
           */
          newFileData = fromCdr.clone();
          newFileData.setOffset(-1);
          newFileData.resetDeferredCrc();
        } catch (CloneNotSupportedException e) {
          throw new IOException("Failed to clone CDR.", e);
        }

        /*
         * Read the data (read directly the compressed source if there is one).
         */
        ProcessedAndRawByteSources fromSource = fromEntry.getSource();
        InputStream fromInput = fromSource.getRawByteSource().openStream();
        long sourceSize = fromSource.getRawByteSource().size();
        if (sourceSize > Integer.MAX_VALUE) {
          throw new IOException("Cannot read source with " + sourceSize + " bytes.");
        }

        byte[] data = new byte[Ints.checkedCast(sourceSize)];
        int read = 0;
        while (read < data.length) {
          int r = fromInput.read(data, read, data.length - read);
          Verify.verify(r >= 0, "There should be at least 'size' bytes in the stream.");
          read += r;
        }

        /*
         * Build the new source and wrap it around an inflater source if data came from
         * a compressed source.
         */
        CloseableByteSource rawContents = storage.fromSource(fromSource.getRawByteSource());
        CloseableByteSource processedContents;
        if (fromCompressInfo.getMethod() == CompressionMethod.DEFLATE) {
          //noinspection IOResourceOpenedButNotSafelyClosed
          processedContents = new InflaterByteSource(rawContents);
        } else {
          processedContents = rawContents;
        }

        ProcessedAndRawByteSources newSource =
            new ProcessedAndRawByteSources(processedContents, rawContents);

        /*
         * Add will replace any current entry with the same name.
         */
        StoredEntry newEntry = new StoredEntry(newFileData, this, newSource, storage);
        add(newEntry);
      }
    }
  }

  public void touch() {
    checkNotInReadOnlyMode();
    dirty = true;
  }

  public void finishAllBackgroundTasks() throws IOException {
    processAllReadyEntriesWithWait();
  }

  public boolean realign() throws IOException {
    checkNotInReadOnlyMode();

    boolean anyChanges = false;
    for (StoredEntry entry : entries()) {
      anyChanges |= entry.realign();
    }

    if (anyChanges) {
      dirty = true;
    }

    return anyChanges;
  }

  boolean realign(StoredEntry entry) throws IOException {
    FileUseMapEntry<StoredEntry> mapEntry =
        entries.get(entry.getCentralDirectoryHeader().getName());
    Verify.verify(entry == mapEntry.getStore());
    long currentDataOffset = mapEntry.getStart() + entry.getLocalHeaderSize();

    int expectedAlignment = chooseAlignment(entry);
    long misalignment = currentDataOffset % expectedAlignment;
    if (misalignment == 0) {
      /*
       * Good. File is aligned properly.
       */
      return false;
    }

    if (entry.getCentralDirectoryHeader().getOffset() == -1) {
      /*
       * File is not aligned but it is not written. We do not really need to do much other
       * than find another place in the map.
       */
      map.remove(mapEntry);
      long newStart =
          map.locateFree(
              mapEntry.getSize(),
              entry.getLocalHeaderSize(),
              expectedAlignment,
              FileUseMap.PositionAlgorithm.BEST_FIT);
      mapEntry = map.add(newStart, newStart + entry.getInFileSize(), entry);
      entries.put(entry.getCentralDirectoryHeader().getName(), mapEntry);

      /*
       * Just for safety. We're modifying the in-memory structures but the file should
       * already be marked as dirty.
       */
      Verify.verify(dirty);

      return false;
    }

    /*
     * Get the entry data source, but check if we have a compressed one (we don't want to
     * inflate and deflate).
     */
    CentralDirectoryHeaderCompressInfo compressInfo =
        entry.getCentralDirectoryHeader().getCompressionInfoWithWait();

    ProcessedAndRawByteSources source = entry.getSource();

    CentralDirectoryHeader clonedCdh;
    try {
      clonedCdh = entry.getCentralDirectoryHeader().clone();
    } catch (CloneNotSupportedException e) {
      Verify.verify(false);
      return false;
    }

    /*
     * We make two changes in the central directory when realigning:
     * we reset the offset to force the entry to be written and we reset the
     * deferred CRC bit as we don't need the extra stuff after the file. It takes
     * space and is totally useless and we may need the extra space to realign the entry...
     */
    clonedCdh.setOffset(-1);
    clonedCdh.resetDeferredCrc();

    CloseableByteSource rawContents = storage.fromSource(source.getRawByteSource());
    CloseableByteSource processedContents;

    if (compressInfo.getMethod() == CompressionMethod.DEFLATE) {
      //noinspection IOResourceOpenedButNotSafelyClosed
      processedContents = new InflaterByteSource(rawContents);
    } else {
      processedContents = rawContents;
    }

    ProcessedAndRawByteSources newSource =
        new ProcessedAndRawByteSources(processedContents, rawContents);

    /*
     * Add the new file. This will replace the existing one.
     */
    StoredEntry newEntry = new StoredEntry(clonedCdh, this, newSource, storage);
    add(newEntry);
    return true;
  }

  public void addZFileExtension(ZFileExtension extension) {
    checkNotInReadOnlyMode();
    extensions.add(extension);
  }

  public void removeZFileExtension(ZFileExtension extension) {
    checkNotInReadOnlyMode();
    extensions.remove(extension);
  }

  private void notify(IOExceptionFunction<ZFileExtension, IOExceptionRunnable> function)
      throws IOException {
    for (ZFileExtension fl : Lists.newArrayList(extensions)) {
      IOExceptionRunnable r = function.apply(fl);
      if (r != null) {
        toRun.add(r);
      }
    }

    if (!isNotifying) {
      isNotifying = true;

      try {
        while (!toRun.isEmpty()) {
          IOExceptionRunnable r = toRun.remove(0);
          r.run();
        }
      } finally {
        isNotifying = false;
      }
    }
  }

  public void directWrite(long offset, byte[] data, int start, int count) throws IOException {
    checkNotInReadOnlyMode();

    Preconditions.checkArgument(offset >= 0, "offset < 0");
    Preconditions.checkArgument(start >= 0, "start >= 0");
    Preconditions.checkArgument(count >= 0, "count >= 0");

    if (data.length == 0) {
      return;
    }

    Preconditions.checkArgument(start <= data.length, "start > data.length");
    Preconditions.checkArgument(start + count <= data.length, "start + count > data.length");

    reopenRw();
    Preconditions.checkNotNull(raf, "raf == null");

    raf.seek(offset);
    raf.write(data, start, count);
  }

  public void directWrite(long offset, byte[] data) throws IOException {
    directWrite(offset, data, 0, data.length);
  }

  public long directSize() throws IOException {
    /*
     * Only force a reopen if the file is closed.
     */
    if (raf == null) {
      reopenRw();
      Preconditions.checkNotNull(raf, "raf == null");
    }
    return raf.length();
  }

  public int directRead(long offset, byte[] data, int start, int count) throws IOException {
    Preconditions.checkArgument(start >= 0, "start >= 0");
    Preconditions.checkArgument(count >= 0, "count >= 0");
    Preconditions.checkArgument(start <= data.length, "start > data.length");
    Preconditions.checkArgument(start + count <= data.length, "start + count > data.length");
    return directRead(offset, ByteBuffer.wrap(data, start, count));
  }

  public int directRead(long offset, ByteBuffer dest) throws IOException {
    Preconditions.checkArgument(offset >= 0, "offset < 0");

    if (!dest.hasRemaining()) {
      return 0;
    }

    /*
     * Only force a reopen if the file is closed.
     */
    if (raf == null) {
      reopenRw();
      Preconditions.checkNotNull(raf, "raf == null");
    }

    raf.seek(offset);
    return raf.getChannel().read(dest);
  }

  public int directRead(long offset, byte[] data) throws IOException {
    return directRead(offset, data, 0, data.length);
  }

  public void directFullyRead(long offset, byte[] data) throws IOException {
    directFullyRead(offset, ByteBuffer.wrap(data));
  }

  public void directFullyRead(long offset, ByteBuffer dest) throws IOException {
    Preconditions.checkArgument(offset >= 0, "offset < 0");

    if (!dest.hasRemaining()) {
      return;
    }

    /*
     * Only force a reopen if the file is closed.
     */
    if (raf == null) {
      reopenRw();
      Preconditions.checkNotNull(raf, "raf == null");
    }

    FileChannel fileChannel = raf.getChannel();
    while (dest.hasRemaining()) {
      fileChannel.position(offset);
      int chunkSize = fileChannel.read(dest);
      if (chunkSize == -1) {
        throw new EOFException("Failed to read " + dest.remaining() + " more bytes: premature EOF");
      }
      offset += chunkSize;
    }
  }

  public void addAllRecursively(File file) throws IOException {
    checkNotInReadOnlyMode();
    addAllRecursively(file, f -> true);
  }

  public void addAllRecursively(File file, Predicate<? super File> mayCompress) throws IOException {
    checkNotInReadOnlyMode();

    addAllRecursively(file, file, mayCompress);
  }

  private void addAllRecursively(File file, File base, Predicate<? super File> mayCompress)
      throws IOException {
    // If we're just adding a file, do not compute a relative path, but rather use the file name
    // as path.
    String path =
        Objects.equal(file, base)
            ? file.getName()
            : base.toURI().relativize(file.toURI()).getPath();

    /*
     * The case of file.isFile() is different because if file.isFile() we will add it to the
     * zip in the root. However, if file.isDirectory() we won't add it and add its children.
     */
    if (file.isFile()) {
      boolean mayCompressFile = mayCompress.apply(file);

      try (Closer closer = Closer.create()) {
        FileInputStream fileInput = closer.register(new FileInputStream(file));
        add(path, fileInput, mayCompressFile);
      }

      return;
    } else if (file.isDirectory()) {
      // Add an entry for the directory, unless it is the base.
      if (!file.equals(base)) {
        try (Closer closer = Closer.create()) {
          InputStream stream = closer.register(new ByteArrayInputStream(new byte[0]));
          add(path, stream, false);
        }
      }

      // Add recursively.
      File[] directoryContents = file.listFiles();
      if (directoryContents != null) {
        Arrays.sort(directoryContents, (f0, f1) -> f0.getName().compareTo(f1.getName()));
        for (File subFile : directoryContents) {
          addAllRecursively(subFile, base, mayCompress);
        }
      }
    }
  }

  public long getCentralDirectoryOffset() {
    if (directoryEntry != null) {
      return directoryEntry.getStart();
    }

    /*
     * If there are no entries, the central directory is written at the start of the file.
     */
    if (entries.isEmpty()) {
      return extraDirectoryOffset;
    }

    /*
     * The Central Directory is written after all entries. This will be at the end of the file
     * if the
     */
    return map.usedSize() + extraDirectoryOffset;
  }

  public long getCentralDirectorySize() {
    if (directoryEntry != null) {
      return directoryEntry.getSize();
    }

    if (entries.isEmpty()) {
      return 0;
    }

    return 1;
  }

  public long getEocdOffset() {
    if (eocdEntry == null) {
      return -1;
    }

    return eocdEntry.getStart();
  }

  public long getEocdSize() {
    if (eocdEntry == null) {
      return -1;
    }

    return eocdEntry.getSize();
  }

  public byte[] getEocdComment() {
    if (eocdEntry == null) {
      Verify.verify(eocdComment != null);
      byte[] eocdCommentCopy = new byte[eocdComment.length];
      System.arraycopy(eocdComment, 0, eocdCommentCopy, 0, eocdComment.length);
      return eocdCommentCopy;
    }

    Eocd eocd = eocdEntry.getStore();
    Verify.verify(eocd != null);
    return eocd.getComment();
  }

  public void setEocdComment(byte[] comment) {
    checkNotInReadOnlyMode();

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
      if (comment[i] == EOCD_SIGNATURE[3]
          && comment[i + 1] == EOCD_SIGNATURE[2]
          && comment[i + 2] == EOCD_SIGNATURE[1]
          && comment[i + 3] == EOCD_SIGNATURE[0]) {
        // We found a possible EOCD signature at position i. Try to read it.
        ByteBuffer bytes = ByteBuffer.wrap(comment, i, comment.length - i);
        try {
          new Eocd(bytes);
          throw new IllegalArgumentException(
              "Position " + i + " of the comment contains a valid EOCD record.");
        } catch (IOException e) {
          // Fine, this is an invalid record. Move along...
        }
      }
    }

    deleteDirectoryAndEocd();
    eocdComment = new byte[comment.length];
    System.arraycopy(comment, 0, eocdComment, 0, comment.length);
    dirty = true;
  }

  public void setExtraDirectoryOffset(long offset) {
    checkNotInReadOnlyMode();
    Preconditions.checkArgument(offset >= 0, "offset < 0");

    if (extraDirectoryOffset != offset) {
      extraDirectoryOffset = offset;
      deleteDirectoryAndEocd();
      dirty = true;
    }
  }

  public long getExtraDirectoryOffset() {
    return extraDirectoryOffset;
  }

  public boolean areTimestampsIgnored() {
    return noTimestamps;
  }

  public void sortZipContents() throws IOException {
    checkNotInReadOnlyMode();
    reopenRw();

    processAllReadyEntriesWithWait();

    Verify.verify(uncompressedEntries.isEmpty());

    SortedSet<StoredEntry> sortedEntries = Sets.newTreeSet(StoredEntry.COMPARE_BY_NAME);
    for (FileUseMapEntry<StoredEntry> fmEntry : entries.values()) {
      StoredEntry entry = fmEntry.getStore();
      Preconditions.checkNotNull(entry);
      sortedEntries.add(entry);
      entry.loadSourceIntoMemory();

      map.remove(fmEntry);
    }

    entries.clear();
    for (StoredEntry entry : sortedEntries) {
      String name = entry.getCentralDirectoryHeader().getName();
      FileUseMapEntry<StoredEntry> positioned = positionInFile(entry, PositionHint.LOWEST_OFFSET);

      entries.put(name, positioned);
    }

    dirty = true;
  }

  public File getFile() {
    return file;
  }

  public DataSource asDataSource() throws IOException {
    if (raf == null) {
      reopenRw();
      Preconditions.checkNotNull(raf, "raf == null");
    }
    return DataSources.asDataSource(this.raf);
  }

  public DataSource asDataSource(long offset, long size) throws IOException {
    if (raf == null) {
      reopenRw();
      Preconditions.checkNotNull(raf, "raf == null");
    }
    return DataSources.asDataSource(this.raf, offset, size);
  }

  VerifyLog makeVerifyLog() {
    VerifyLog log = verifyLogFactory.get();
    Preconditions.checkNotNull(log, "log == null");
    return log;
  }

  VerifyLog getVerifyLog() {
    return verifyLog;
  }

  public boolean hasPendingChangesWithWait() throws IOException {
    processAllReadyEntriesWithWait();
    return dirty;
  }

  public ByteStorage getStorage() {
    return storage;
  }

  enum PositionHint {
    ANYWHERE,

    LOWEST_OFFSET
  }
}