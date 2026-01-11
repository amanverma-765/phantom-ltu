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

package com.android.tools.build.apkzlib.zfile;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

public interface ApkCreator extends Closeable {

  void writeZip(
      File zip, @Nullable Function<String, String> transform, @Nullable Predicate<String> isIgnored)
      throws IOException;

  void writeFile(File inputFile, String apkPath) throws IOException;

  void deleteFile(String apkPath) throws IOException;

  boolean hasPendingChangesWithWait() throws IOException;
}