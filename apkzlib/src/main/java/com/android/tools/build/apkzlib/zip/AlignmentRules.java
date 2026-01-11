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

public final class AlignmentRules {

  private AlignmentRules() {}

  public static AlignmentRule constant(int alignment) {
    Preconditions.checkArgument(alignment > 0, "alignment <= 0");

    return (String path) -> alignment;
  }

  public static AlignmentRule constantForSuffix(String suffix, int alignment) {
    Preconditions.checkArgument(!suffix.isEmpty(), "suffix.isEmpty()");
    Preconditions.checkArgument(alignment > 0, "alignment <= 0");

    return (String path) -> path.endsWith(suffix) ? alignment : AlignmentRule.NO_ALIGNMENT;
  }

  public static AlignmentRule compose(AlignmentRule... rules) {
    return (String path) -> {
      for (AlignmentRule r : rules) {
        int align = r.alignment(path);
        if (align != AlignmentRule.NO_ALIGNMENT) {
          return align;
        }
      }

      return AlignmentRule.NO_ALIGNMENT;
    };
  }
}
