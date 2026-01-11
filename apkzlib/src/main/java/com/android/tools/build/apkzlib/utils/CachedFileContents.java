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

package com.android.tools.build.apkzlib.utils;

import com.google.common.base.Objects;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

public class CachedFileContents<T> {

  private final File file;

  private long lastClosed;

  private long size;

  @Nullable private HashCode hash;

  @Nullable private T cache;

  public CachedFileContents(File file) {
    this.file = file;
  }

  public void closed(@Nullable T cache) {
    this.cache = cache;
    lastClosed = file.lastModified();
    size = file.length();
    hash = hashFile();
  }

  public boolean isValid() {
    boolean valid = true;

    if (!file.exists()) {
      valid = false;
    }

    if (valid && file.lastModified() != lastClosed) {
      valid = false;
    }

    if (valid && file.length() != size) {
      valid = false;
    }

    if (valid && !Objects.equal(hash, hashFile())) {
      valid = false;
    }

    if (!valid) {
      cache = null;
    }

    return valid;
  }

  @Nullable
  public T getCache() {
    return cache;
  }

  @Nullable
  private HashCode hashFile() {
    try {
      return Files.asByteSource(file).hash(Hashing.crc32());
    } catch (IOException e) {
      return null;
    }
  }

  public File getFile() {
    return file;
  }
}