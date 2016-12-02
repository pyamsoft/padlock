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

import android.content.ContentValues;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;

@AutoValue public abstract class PadLockEntry implements PadLockEntryModel {

  /**
   * The activity name of the PACKAGE entry in the database
   */
  @NonNull public static final String PACKAGE_ACTIVITY_NAME = "PACKAGE";
  // SQLDelight does not yet support delete strings
  @NonNull public static final String DELETE_WITH_PACKAGE_NAME = "packageName = ?";
  @NonNull public static final String DELETE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";
  @NonNull public static final String DELETE_ALL = "1=1";
  // SQLDelight does not yet support update strings
  @NonNull public static final String UPDATE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";

  @SuppressWarnings("StaticInitializerReferencesSubClass") @NonNull
  private static final Factory<PadLockEntry> FACTORY = new Factory<>(AutoValue_PadLockEntry::new);

  @NonNull public static final Creator<PadLockEntry> CREATOR = FACTORY.creator;
  @NonNull public static final RowMapper<AllEntries> ALL_ENTRIES_MAPPER =
      FACTORY.all_entriesMapper(AutoValue_PadLockEntry_AllEntries::new);
  @NonNull public static final RowMapper<WithPackageName> WITH_PACKAGE_NAME_MAPPER =
      FACTORY.with_package_nameMapper(AutoValue_PadLockEntry_WithPackageName::new);
  @NonNull public static final Mapper<PadLockEntry> WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER =
      FACTORY.with_package_activity_name_defaultMapper();
  @NonNull public static final RowMapper<WithPackageActivityName>
      WITH_PACKAGE_ACTIVITY_NAME_MAPPER =
      FACTORY.with_package_activity_nameMapper(AutoValue_PadLockEntry_WithPackageActivityName::new);
  @NonNull private static final String PACKAGE_EMPTY = "EMPTY";
  @NonNull private static final String ACTIVITY_EMPTY = "EMPTY";

  @NonNull @CheckResult public static PadLockEntry empty() {
    return new AutoValue_PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, 0, 0, false, false);
  }

  @CheckResult public static boolean isEmpty(@NonNull PadLockEntry entry) {
    return entry.packageName().equals(PACKAGE_EMPTY) && entry.activityName().equals(ACTIVITY_EMPTY);
  }

  /**
   * Marshal method is deprecated, but Marshal class is not. Keep using it for now I suppose.
   */
  @NonNull @CheckResult public static ContentValues asContentValues(@NonNull PadLockEntry entry) {
    return new Marshal(entry).asContentValues();
  }

  @AutoValue public static abstract class AllEntries implements All_entriesModel {

  }

  @AutoValue public static abstract class WithPackageName implements With_package_nameModel {

  }

  @AutoValue public static abstract class WithPackageActivityName
      implements With_package_activity_nameModel {

    @NonNull @CheckResult public static WithPackageActivityName empty() {
      return new AutoValue_PadLockEntry_WithPackageActivityName(PACKAGE_EMPTY, ACTIVITY_EMPTY, null,
          0, 0, false);
    }

    @CheckResult public static boolean isEmpty(@NonNull WithPackageActivityName entry) {
      return entry.packageName().equals(PACKAGE_EMPTY) && entry.activityName()
          .equals(ACTIVITY_EMPTY);
    }
  }
}
