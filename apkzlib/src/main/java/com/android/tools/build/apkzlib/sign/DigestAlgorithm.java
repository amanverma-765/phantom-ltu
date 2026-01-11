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

package com.android.tools.build.apkzlib.sign;


public enum DigestAlgorithm {
  SHA1("SHA1", "SHA-1"),

  SHA256("SHA-256", "SHA-256");

  public static final int API_SHA_256_RSA_AND_ECDSA = 18;

  public static final int API_SHA_256_ALL_ALGORITHMS = 21;

  public final String messageDigestName;

  public final String manifestAttributeName;

  public final String entryAttributeName;

  DigestAlgorithm(String attributeName, String messageDigestName) {
    this.messageDigestName = messageDigestName;
    this.entryAttributeName = attributeName + "-Digest";
    this.manifestAttributeName = attributeName + "-Digest-Manifest";
  }
}
