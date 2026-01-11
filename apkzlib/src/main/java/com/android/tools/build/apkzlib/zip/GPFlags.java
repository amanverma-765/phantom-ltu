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

import java.io.IOException;

class GPFlags {

  private static final int BIT_ENCRYPTION = 1;

  private static final int BIT_DEFERRED_CRC = (1 << 3);

  private static final int BIT_ENHANCED_DEFLATING = (1 << 4);

  private static final int BIT_PATCHED_DATA = (1 << 5);

  private static final int BIT_STRONG_ENCRYPTION = (1 << 6) | (1 << 13);

  private static final int BIT_EFS = (1 << 11);

  private static final int BIT_UNUSED =
      (1 << 7) | (1 << 8) | (1 << 9) | (1 << 10) | (1 << 14) | (1 << 15);

  private final long value;

  private boolean deferredCrc;

  private boolean utf8FileName;

  private GPFlags(long value) {
    this.value = value;

    deferredCrc = ((value & BIT_DEFERRED_CRC) != 0);
    utf8FileName = ((value & BIT_EFS) != 0);
  }

  public long getValue() {
    return value;
  }

  public boolean isDeferredCrc() {
    return deferredCrc;
  }

  public boolean isUtf8FileName() {
    return utf8FileName;
  }

  static GPFlags make(boolean utf8Encoding) {
    long flags = 0;

    if (utf8Encoding) {
      flags |= BIT_EFS;
    }

    return new GPFlags(flags);
  }

  static GPFlags from(long bits) throws IOException {
    if ((bits & BIT_ENCRYPTION) != 0) {
      throw new IOException("Zip files with encrypted of entries not supported.");
    }

    if ((bits & BIT_ENHANCED_DEFLATING) != 0) {
      throw new IOException("Enhanced deflating not supported.");
    }

    if ((bits & BIT_PATCHED_DATA) != 0) {
      throw new IOException("Compressed patched data not supported.");
    }

    if ((bits & BIT_STRONG_ENCRYPTION) != 0) {
      throw new IOException("Strong encryption not supported.");
    }

    if ((bits & BIT_UNUSED) != 0) {
      throw new IOException(
          "Unused bits set in directory entry. Weird. I don't know what's " + "going on.");
    }

    if ((bits & 0xffffffff00000000L) != 0) {
      throw new IOException("Unsupported bits after 32.");
    }

    return new GPFlags(bits);
  }
}
