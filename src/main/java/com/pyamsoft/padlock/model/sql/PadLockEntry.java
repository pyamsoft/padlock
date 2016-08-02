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

package com.pyamsoft.padlock.model.sql;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue public abstract class PadLockEntry implements PadLockEntryModel {

  @NonNull public static final String PACKAGE_TAG = "PACKAGE";
  @NonNull public static final String PACKAGE_EMPTY = "EMPTY";
  @NonNull public static final String ACTIVITY_EMPTY = "EMPTY";
  @NonNull public static final Factory<PadLockEntry> FACTORY =
      new Factory<>(AutoValue_PadLockEntry::new);

  @NonNull @CheckResult public static PadLockEntry empty() {
    return new AutoValue_PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, 0, 0, false, false);
  }

  @CheckResult public static boolean isEmpty(@NonNull PadLockEntry entry) {
    return entry.packageName().equals(PACKAGE_EMPTY) && entry.activityName().equals(ACTIVITY_EMPTY);
  }

  // SQLDelight does not yet support delete strings
  @NonNull public static final String DELETE_WITH_PACKAGE_NAME = "packageName = ?";
  @NonNull public static final String DELETE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";
  @NonNull public static final String DELETE_ALL = "1=1";

  // SQLDelight does not yet support update strings
  @NonNull public static final String UPDATE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";
}
