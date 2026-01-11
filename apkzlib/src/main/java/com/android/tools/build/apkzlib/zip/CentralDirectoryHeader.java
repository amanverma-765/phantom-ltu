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

import com.android.tools.build.apkzlib.zip.utils.MsDosDateTimeUtils;
import com.google.common.base.Verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CentralDirectoryHeader implements Cloneable {

  private static final int DEFAULT_VERSION_MADE_BY = 0x0018;

  private static final byte[] EMPTY_COMMENT = new byte[0];

  private final String name;

  private long crc32;

  private long uncompressedSize;

  private long madeBy;

  private GPFlags gpBit;

  private long lastModTime;

  private long lastModDate;

  private ExtraField extraField;

  private byte[] comment;

  private long internalAttributes;

  private long externalAttributes;

  private long offset;

  private byte[] encodedFileName;

  private final Future<CentralDirectoryHeaderCompressInfo> compressInfo;

  private final ZFile file;

  CentralDirectoryHeader(
      String name,
      byte[] encodedFileName,
      long uncompressedSize,
      Future<CentralDirectoryHeaderCompressInfo> compressInfo,
      GPFlags flags,
      ZFile zFile) {
    this(
        name,
        encodedFileName,
        uncompressedSize,
        compressInfo,
        flags,
        zFile,
        MsDosDateTimeUtils.packCurrentTime(),
        MsDosDateTimeUtils.packCurrentDate());
  }

  CentralDirectoryHeader(
      String name,
      byte[] encodedFileName,
      long uncompressedSize,
      Future<CentralDirectoryHeaderCompressInfo> compressInfo,
      GPFlags flags,
      ZFile zFile,
      long currentTime,
      long currentDate) {
    this.name = name;
    this.uncompressedSize = uncompressedSize;
    crc32 = 0;

    /*
     * Set sensible defaults for the rest.
     */
    madeBy = DEFAULT_VERSION_MADE_BY;

    gpBit = flags;
    lastModTime = currentTime;
    lastModDate = currentDate;
    extraField = ExtraField.EMPTY;
    comment = EMPTY_COMMENT;
    internalAttributes = 0;
    externalAttributes = 0;
    offset = -1;
    this.encodedFileName = encodedFileName;
    this.compressInfo = compressInfo;
    file = zFile;
  }

  public CentralDirectoryHeader link(String name, byte[] encodedFileName, GPFlags flags, ZFile file) {
    var newData = new CentralDirectoryHeader(name,
      encodedFileName,
      uncompressedSize,
      compressInfo,
      flags,
      file,
      lastModTime,
      lastModDate);
    newData.extraField = extraField;
    newData.offset = -1;
    newData.internalAttributes = internalAttributes;
    newData.externalAttributes = externalAttributes;
    newData.comment = comment;
    newData.madeBy = madeBy;
    newData.crc32 = crc32;
    return newData;
  }

  public String getName() {
    return name;
  }

  public long getUncompressedSize() {
    return uncompressedSize;
  }

  public long getCrc32() {
    return crc32;
  }

  void setCrc32(long crc32) {
    this.crc32 = crc32;
  }

  public long getMadeBy() {
    return madeBy;
  }

  void setMadeBy(long madeBy) {
    this.madeBy = madeBy;
  }

  public GPFlags getGpBit() {
    return gpBit;
  }

  public long getLastModTime() {
    return lastModTime;
  }

  void setLastModTime(long lastModTime) {
    this.lastModTime = lastModTime;
  }

  public long getLastModDate() {
    return lastModDate;
  }

  void setLastModDate(long lastModDate) {
    this.lastModDate = lastModDate;
  }

  public ExtraField getExtraField() {
    return extraField;
  }

  public void setExtraField(ExtraField extraField) {
    setExtraFieldNoNotify(extraField);
    file.centralDirectoryChanged();
  }

  void setExtraFieldNoNotify(ExtraField extraField) {
    this.extraField = extraField;
  }

  public byte[] getComment() {
    return comment;
  }

  void setComment(byte[] comment) {
    this.comment = comment;
  }

  public long getInternalAttributes() {
    return internalAttributes;
  }

  void setInternalAttributes(long internalAttributes) {
    this.internalAttributes = internalAttributes;
  }

  public long getExternalAttributes() {
    return externalAttributes;
  }

  void setExternalAttributes(long externalAttributes) {
    this.externalAttributes = externalAttributes;
  }

  public long getOffset() {
    return offset;
  }

  void setOffset(long offset) {
    this.offset = offset;
  }

  public byte[] getEncodedFileName() {
    return encodedFileName;
  }

  void resetDeferredCrc() {
    /*
     * We actually create a new set of flags. Since the only information we care about is the
     * UTF-8 encoding, we'll just create a brand new object.
     */
    gpBit = GPFlags.make(gpBit.isUtf8FileName());
  }

  @Override
  protected CentralDirectoryHeader clone() throws CloneNotSupportedException {
    CentralDirectoryHeader cdr = (CentralDirectoryHeader) super.clone();
    cdr.extraField = extraField;
    cdr.comment = Arrays.copyOf(comment, comment.length);
    cdr.encodedFileName = Arrays.copyOf(encodedFileName, encodedFileName.length);
    return cdr;
  }

  public Future<CentralDirectoryHeaderCompressInfo> getCompressionInfo() {
    return compressInfo;
  }

  public CentralDirectoryHeaderCompressInfo getCompressionInfoWithWait() throws IOException {
    try {
      CentralDirectoryHeaderCompressInfo info = getCompressionInfo().get();
      Verify.verifyNotNull(info, "info == null");
      return info;
    } catch (InterruptedException e) {
      throw new IOException("Interrupted while waiting for compression information.", e);
    } catch (ExecutionException e) {
      throw new IOException("Execution of compression failed.", e);
    }
  }
}