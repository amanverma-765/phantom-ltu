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

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

class FileUseMap {
  private long size;

  private final TreeSet<FileUseMapEntry<?>> map;

  private final TreeSet<FileUseMapEntry<?>> freeBySize;
  private final TreeSet<FileUseMapEntry<?>> freeByStart;

  private int mMinFreeSize;

  FileUseMap(long size, int minFreeSize) {
    Preconditions.checkArgument(size >= 0, "size < 0");
    Preconditions.checkArgument(minFreeSize >= 0, "minFreeSize < 0");

    this.size = size;
    map = new TreeSet<>(FileUseMapEntry.COMPARE_BY_START);
    freeBySize = new TreeSet<>(FileUseMapEntry.COMPARE_BY_SIZE);
    freeByStart = new TreeSet<>(FileUseMapEntry.COMPARE_BY_START);
    mMinFreeSize = minFreeSize;

    if (size > 0) {
      internalAdd(FileUseMapEntry.makeFree(0, size));
    }
  }

  private void internalAdd(FileUseMapEntry<?> entry) {
    map.add(entry);

    if (entry.isFree()) {
      freeBySize.add(entry);
      freeByStart.add(entry);
    }
  }

  private void internalRemove(FileUseMapEntry<?> entry) {
    boolean wasRemoved = map.remove(entry);
    Preconditions.checkState(wasRemoved, "entry not in map");

    if (entry.isFree()) {
      freeBySize.remove(entry);
      freeByStart.remove(entry);
    }
  }

  private void add(FileUseMapEntry<?> entry) {
    Preconditions.checkArgument(entry.getStart() < size, "entry.getStart() >= size");
    Preconditions.checkArgument(entry.getEnd() <= size, "entry.getEnd() > size");
    Preconditions.checkArgument(!entry.isFree(), "entry.isFree()");

    FileUseMapEntry<?> container = findContainer(entry);
    Verify.verify(container.isFree(), "!container.isFree()");

    Set<FileUseMapEntry<?>> replacements = split(container, entry);
    internalRemove(container);
    for (FileUseMapEntry<?> r : replacements) {
      internalAdd(r);
    }
  }

  <T> FileUseMapEntry<T> add(long start, long end, T store) {
    Preconditions.checkArgument(start >= 0, "start < 0");
    Preconditions.checkArgument(end > start, "end < start");

    FileUseMapEntry<T> entry = FileUseMapEntry.makeUsed(start, end, store);
    add(entry);
    return entry;
  }

  void remove(FileUseMapEntry<?> entry) {
    Preconditions.checkState(map.contains(entry), "!map.contains(entry)");
    Preconditions.checkArgument(!entry.isFree(), "entry.isFree()");

    internalRemove(entry);

    FileUseMapEntry<?> replacement = FileUseMapEntry.makeFree(entry.getStart(), entry.getEnd());
    internalAdd(replacement);
    coalesce(replacement);
  }

  private FileUseMapEntry<?> findContainer(FileUseMapEntry<?> entry) {
    FileUseMapEntry container = map.floor(entry);
    Verify.verifyNotNull(container);
    Verify.verify(container.getStart() <= entry.getStart());
    Verify.verify(container.getEnd() >= entry.getEnd());

    return container;
  }

  private static Set<FileUseMapEntry<?>> split(
      FileUseMapEntry<?> container, FileUseMapEntry<?> entry) {
    Preconditions.checkArgument(container.isFree(), "!container.isFree()");

    long farStart = container.getStart();
    long start = entry.getStart();
    long end = entry.getEnd();
    long farEnd = container.getEnd();

    Verify.verify(farStart <= start, "farStart > start");
    Verify.verify(start < end, "start >= end");
    Verify.verify(farEnd >= end, "farEnd < end");

    Set<FileUseMapEntry<?>> result = Sets.newHashSet();
    if (farStart < start) {
      result.add(FileUseMapEntry.makeFree(farStart, start));
    }

    result.add(entry);

    if (end < farEnd) {
      result.add(FileUseMapEntry.makeFree(end, farEnd));
    }

    return result;
  }

  private void coalesce(FileUseMapEntry<?> entry) {
    Preconditions.checkArgument(entry.isFree(), "!entry.isFree()");

    FileUseMapEntry<?> prevToMerge = null;
    long start = entry.getStart();
    if (start > 0) {
      /*
       * See if we have a previous entry to merge with this one.
       */
      prevToMerge = map.floor(FileUseMapEntry.makeFree(start - 1, start));
      Verify.verifyNotNull(prevToMerge);
      if (!prevToMerge.isFree()) {
        prevToMerge = null;
      }
    }

    FileUseMapEntry<?> nextToMerge = null;
    long end = entry.getEnd();
    if (end < size) {
      /*
       * See if we have a next entry to merge with this one.
       */
      nextToMerge = map.ceiling(FileUseMapEntry.makeFree(end, end + 1));
      Verify.verifyNotNull(nextToMerge);
      if (!nextToMerge.isFree()) {
        nextToMerge = null;
      }
    }

    if (prevToMerge == null && nextToMerge == null) {
      return;
    }

    long newStart = start;
    if (prevToMerge != null) {
      newStart = prevToMerge.getStart();
      internalRemove(prevToMerge);
    }

    long newEnd = end;
    if (nextToMerge != null) {
      newEnd = nextToMerge.getEnd();
      internalRemove(nextToMerge);
    }

    internalRemove(entry);
    internalAdd(FileUseMapEntry.makeFree(newStart, newEnd));
  }

  void truncate() {
    if (size == 0) {
      return;
    }

    /*
     * Find the last entry.
     */
    FileUseMapEntry<?> last = map.last();
    Verify.verifyNotNull(last, "last == null");
    if (last.isFree()) {
      internalRemove(last);
      size = last.getStart();
    }
  }

  long size() {
    return size;
  }

  long usedSize() {
    if (size == 0) {
      return 0;
    }

    /*
     * Find the last entry to see if it is an empty entry. If it is, we need to remove its size
     * from the returned value.
     */
    FileUseMapEntry<?> last = map.last();
    Verify.verifyNotNull(last, "last == null");
    if (last.isFree()) {
      return last.getStart();
    } else {
      Verify.verify(last.getEnd() == size);
      return size;
    }
  }

  void extend(long size) {
    Preconditions.checkArgument(size >= this.size, "size < size");

    if (this.size == size) {
      return;
    }

    FileUseMapEntry<?> newBlock = FileUseMapEntry.makeFree(this.size, size);
    internalAdd(newBlock);

    this.size = size;

    coalesce(newBlock);
  }

  long locateFree(long size, long alignOffset, long align, PositionAlgorithm alg) {
    Preconditions.checkArgument(size > 0, "size <= 0");

    FileUseMapEntry<?> minimumSizedEntry = FileUseMapEntry.makeFree(0, size);
    SortedSet<FileUseMapEntry<?>> matches;

    switch (alg) {
      case BEST_FIT:
        matches = freeBySize.tailSet(minimumSizedEntry);
        break;
      case FIRST_FIT:
        matches = freeByStart;
        break;
      default:
        throw new AssertionError();
    }

    FileUseMapEntry<?> best = null;
    long bestExtraSize = 0;
    for (FileUseMapEntry<?> curr : matches) {
      /*
       * We don't care about blocks that aren't free.
       */
      if (!curr.isFree()) {
        continue;
      }

      /*
       * Compute any extra size we need in this block to make sure we verify the alignment.
       * There must be a better to do this...
       */
      long extraSize;
      if (align == 0) {
        extraSize = 0;
      } else {
        extraSize = (align - ((curr.getStart() + alignOffset) % align)) % align;
      }

      /*
       * We can't leave than mMinFreeSize before. So if the extraSize is less than
       * mMinFreeSize, we have to increase it by 'align' as many times as needed. For
       * example, if mMinFreeSize is 20, align 4 and extraSize is 5. We need to increase it
       * to 21 (5 + 4 * 4)
       */
      if (extraSize > 0 && extraSize < mMinFreeSize) {
        int addAlignBlocks = Ints.checkedCast((mMinFreeSize - extraSize + align - 1) / align);
        extraSize += addAlignBlocks * align;
      }

      /*
       * We don't care about blocks where we don't fit in.
       */
      if (curr.getSize() < (size + extraSize)) {
        continue;
      }

      /*
       * We don't care about blocks that leave less than the minimum size after. There are
       * two exceptions: (1) this is the last block and (2) the next block is free in which
       * case, after coalescing, the free block with have at least the minimum size.
       */
      long emptySpaceLeft = curr.getSize() - (size + extraSize);
      if (emptySpaceLeft > 0 && emptySpaceLeft < mMinFreeSize) {
        FileUseMapEntry<?> next = map.higher(curr);
        if (next != null && !next.isFree()) {
          continue;
        }
      }

      /*
       * We don't care about blocks that are bigger than the best so far (otherwise this
       * wouldn't be a best-fit algorithm).
       */
      if (best != null && best.getSize() < curr.getSize()) {
        continue;
      }

      best = curr;
      bestExtraSize = extraSize;

      /*
       * If we're doing first fit, we don't want to search for a better one :)
       */
      if (alg == PositionAlgorithm.FIRST_FIT) {
        break;
      }
    }

    /*
     * If no entry that could hold size is found, get the first free byte.
     */
    long firstFree = this.size;
    if (best == null && !map.isEmpty()) {
      FileUseMapEntry<?> last = map.last();
      if (last.isFree()) {
        firstFree = last.getStart();
      }
    }

    /*
     * We're done: either we found something or we didn't, in which the new entry needs to
     * be added to the end of the map.
     */
    if (best == null) {
      long extra = (align - ((firstFree + alignOffset) % align)) % align;

      /*
       * If adding this entry at the end would create a space smaller than the minimum,
       * push it for 'align' bytes forward.
       */
      if (extra > 0) {
        if (extra < mMinFreeSize) {
          extra += align * (((mMinFreeSize - extra) + (align - 1)) / align);
        }
      }

      return firstFree + extra;
    } else {
      return best.getStart() + bestExtraSize;
    }
  }

  List<FileUseMapEntry<?>> getFreeAreas() {
    List<FileUseMapEntry<?>> freeAreas = Lists.newArrayList();

    for (FileUseMapEntry<?> area : map) {
      if (area.isFree() && area.getEnd() != size) {
        freeAreas.add(area);
      }
    }

    return freeAreas;
  }

  @Nullable
  FileUseMapEntry<?> before(FileUseMapEntry<?> entry) {
    Preconditions.checkNotNull(entry, "entry == null");

    return map.lower(entry);
  }

  @Nullable
  FileUseMapEntry<?> after(FileUseMapEntry<?> entry) {
    Preconditions.checkNotNull(entry, "entry == null");

    return map.higher(entry);
  }

  @Nullable
  FileUseMapEntry<?> at(long offset) {
    Preconditions.checkArgument(offset >= 0, "offset < 0");
    Preconditions.checkArgument(offset < size, "offset >= size");

    FileUseMapEntry<?> entry = map.floor(FileUseMapEntry.makeFree(offset, offset + 1));
    if (entry == null) {
      return null;
    }

    Verify.verify(entry.getStart() <= offset);
    Verify.verify(entry.getEnd() > offset);

    return entry;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (FileUseMapEntry<?> entry : map) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }

      builder.append(entry.getStart());
      builder.append(" - ");
      builder.append(entry.getEnd());
      builder.append(": ");
      builder.append(entry.getStore());
    }
    return builder.toString();
  }

  public enum PositionAlgorithm {
    BEST_FIT,

    FIRST_FIT
  }
}