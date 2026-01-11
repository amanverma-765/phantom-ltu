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

package com.android.tools.build.apkzlib.bytestorage;

import java.io.IOException;
import java.io.InputStream;

class LimitedInputStream extends InputStream {
  private final InputStream input;

  private long remaining;

  private boolean eofDetected;

  LimitedInputStream(InputStream input, long maximum) {
    this.input = input;
    this.remaining = maximum;
    this.eofDetected = false;
  }

  @Override
  public int read() throws IOException {
    if (remaining == 0) {
      return -1;
    }

    int r = input.read();
    if (r >= 0) {
      remaining--;
    } else {
      eofDetected = true;
    }

    return r;
  }

  @Override
  public int read(byte[] whereTo, int offset, int length) throws IOException {
    if (remaining == 0) {
      return -1;
    }

    int toRead = (int) Math.min(remaining, length);
    int r = input.read(whereTo, offset, toRead);
    if (r >= 0) {
      remaining -= r;
    } else {
      eofDetected = true;
    }

    return r;
  }

  boolean isInputFinished() {
    return eofDetected;
  }
}
