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

import java.security.NoSuchAlgorithmException;

public enum SignatureAlgorithm {
  RSA("RSA", 1, "withRSA"),

  ECDSA("EC", 18, "withECDSA"),

  DSA("DSA", 1, "withDSA");

  public final String keyAlgorithm;

  public final int minSdkVersion;

  public final String signatureAlgorithmSuffix;

  SignatureAlgorithm(String keyAlgorithm, int minSdkVersion, String signatureAlgorithmSuffix) {
    this.keyAlgorithm = keyAlgorithm;
    this.minSdkVersion = minSdkVersion;
    this.signatureAlgorithmSuffix = signatureAlgorithmSuffix;
  }

  public static SignatureAlgorithm fromKeyAlgorithm(String keyAlgorithm, int minSdkVersion)
      throws NoSuchAlgorithmException {
    for (SignatureAlgorithm alg : values()) {
      if (alg.keyAlgorithm.equalsIgnoreCase(keyAlgorithm)) {
        if (alg.minSdkVersion > minSdkVersion) {
          throw new NoSuchAlgorithmException(
              "Signatures with "
                  + keyAlgorithm
                  + " keys are not supported on minSdkVersion "
                  + minSdkVersion
                  + ". They are supported only for minSdkVersion >= "
                  + alg.minSdkVersion);
        }

        return alg;
      }
    }

    throw new NoSuchAlgorithmException("Signing with " + keyAlgorithm + " keys is not supported");
  }

  public String signatureAlgorithmName(DigestAlgorithm digestAlgorithm) {
    return digestAlgorithm.messageDigestName.replace("-", "") + signatureAlgorithmSuffix;
  }
}
