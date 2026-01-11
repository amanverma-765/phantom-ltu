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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.util.Comparator;

import javax.annotation.Nullable;

class FileUseMapEntry<T> {

  public static final Comparator<FileUseMapEntry<?>> COMPARE_BY_START =
      (o1, o2) -> Ints.saturatedCast(o1.getStart() - o2.getStart());

  public static final Comparator<FileUseMapEntry<?>> COMPARE_BY_SIZE =
      (o1, o2) -> Ints.saturatedCast(o1.getSize() - o2.getSize());

  private final long start;

  private final long end;

  @Nullable private final T store;

  private FileUseMapEntry(long start, long end, @Nullable T store) {
    Preconditions.checkArgument(start >= 0, "start < 0");
    Preconditions.checkArgument(end > start, "end <= start");

    this.start = start;
    this.end = end;
    this.store = store;
  }

  public static FileUseMapEntry<Object> makeFree(long start, long end) {
    return new FileUseMapEntry<>(start, end, null);
  }

  public static <T> FileUseMapEntry<T> makeUsed(long start, long end, T store) {
    Preconditions.checkNotNull(store, "store == null");
    return new FileUseMapEntry<>(start, end, store);
  }

  long getStart() {
    return start;
  }

  long getEnd() {
    return end;
  }

  long getSize() {
    return end - start;
  }

  boolean isFree() {
    return store == null;
  }

  @Nullable
  T getStore() {
    return store;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("start", start)
        .add("end", end)
        .add("store", store)
        .toString();
  }
}