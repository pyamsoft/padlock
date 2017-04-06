/*
 * Copyright 2016 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.uicommon;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.andrognito.patternlockview.PatternLockView;
import java.util.List;
import java.util.Locale;

public final class LockCellUtils {

  private LockCellUtils() {
    throw new RuntimeException("No instances");
  }

  @NonNull @CheckResult
  public static String cellPatternToString(@NonNull List<PatternLockView.Dot> cells) {
    StringBuilder builder = new StringBuilder(4);
    for (PatternLockView.Dot cell : cells) {
      String cellString =
          String.format(Locale.getDefault(), "%s%s", cell.getRow(), cell.getColumn());
      builder.append(cellString);
    }
    return builder.toString();
  }
}
