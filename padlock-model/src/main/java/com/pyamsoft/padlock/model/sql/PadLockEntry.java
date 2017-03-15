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

import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;

@AutoValue public abstract class PadLockEntry implements PadLockEntryModel {

  /**
   * The activity name of the PACKAGE entry in the database
   */
  @NonNull public static final String PACKAGE_ACTIVITY_NAME = "PACKAGE";
  @NonNull public static final String PACKAGE_EMPTY = "EMPTY";
  @NonNull public static final String ACTIVITY_EMPTY = "EMPTY";
  @SuppressWarnings("StaticInitializerReferencesSubClass") @NonNull public static final PadLockEntry
      EMPTY = new AutoValue_PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, 0, 0, false, false);
  @SuppressWarnings("StaticInitializerReferencesSubClass") @NonNull
  private static final Factory<PadLockEntry> FACTORY = new Factory<>(AutoValue_PadLockEntry::new);
  @NonNull public static final RowMapper<AllEntries> ALL_ENTRIES_MAPPER =
      FACTORY.all_entriesMapper(AutoValue_PadLockEntry_AllEntries::new);
  @NonNull public static final RowMapper<WithPackageName> WITH_PACKAGE_NAME_MAPPER =
      FACTORY.with_package_nameMapper(AutoValue_PadLockEntry_WithPackageName::new);
  @NonNull public static final Mapper<PadLockEntry> WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER =
      FACTORY.with_package_activity_name_defaultMapper();
  //@NonNull public static final RowMapper<WithPackageActivityName>
  //    WITH_PACKAGE_ACTIVITY_NAME_MAPPER =
  //    FACTORY.with_package_activity_nameMapper(AutoValue_PadLockEntry_WithPackageActivityName::new);

  //@CheckResult @NonNull
  //public static SqlDelightStatement withPackageActivityName(@NonNull String packageName,
  //    @NonNull String activityName) {
  //  return FACTORY.with_package_activity_name(packageName, activityName);
  //}

  @CheckResult @NonNull
  public static SqlDelightStatement withPackageActivityNameDefault(@NonNull String packageName,
      @NonNull String activityName) {
    return FACTORY.with_package_activity_name_default(packageName, PACKAGE_ACTIVITY_NAME,
        activityName, PACKAGE_ACTIVITY_NAME, activityName);
  }

  @CheckResult @NonNull
  public static SqlDelightStatement withPackageName(@NonNull String packageName) {
    return FACTORY.with_package_name(packageName);
  }

  @CheckResult @NonNull public static SqlDelightStatement queryAll() {
    return FACTORY.all_entries();
  }

  @CheckResult @NonNull
  public static PadLockEntry create(@NonNull String packageName, @NonNull String activityName,
      @Nullable String lockCode, long lockUntilTime, long ignoreUntilTime, boolean isSystem,
      boolean whitelist) {
    return FACTORY.creator.create(packageName, activityName, lockCode, lockUntilTime,
        ignoreUntilTime, isSystem, whitelist);
  }

  @CheckResult public static boolean isEmpty(@NonNull PadLockEntry entry) {
    return PACKAGE_EMPTY.equals(entry.packageName()) && ACTIVITY_EMPTY.equals(entry.activityName());
  }

  @CheckResult @NonNull
  public static InsertManager insertEntry(@NonNull SQLiteOpenHelper openHelper) {
    return new InsertManager(openHelper);
  }

  @CheckResult @NonNull
  public static DeletePackageManager deletePackage(@NonNull SQLiteOpenHelper openHelper) {
    return new DeletePackageManager(openHelper);
  }

  @CheckResult @NonNull public static DeletePackageActivityManager deletePackageActivity(
      @NonNull SQLiteOpenHelper openHelper) {
    return new DeletePackageActivityManager(openHelper);
  }

  @CheckResult @NonNull
  public static UpdateLockTimeManager updateLockTime(@NonNull SQLiteOpenHelper openHelper) {
    return new UpdateLockTimeManager(openHelper);
  }

  @CheckResult @NonNull
  public static UpdateIgnoreTimeManager updateIgnoreTime(@NonNull SQLiteOpenHelper openHelper) {
    return new UpdateIgnoreTimeManager(openHelper);
  }

  @CheckResult @NonNull
  public static UpdateWhitelistManager updateWhitelist(@NonNull SQLiteOpenHelper openHelper) {
    return new UpdateWhitelistManager(openHelper);
  }

  @AutoValue public static abstract class AllEntries implements All_entriesModel {

  }

  @AutoValue public static abstract class WithPackageName implements With_package_nameModel {

  }

  //@AutoValue public static abstract class WithPackageActivityName
  //    implements With_package_activity_nameModel {
  //
  //  @NonNull @CheckResult public static WithPackageActivityName empty() {
  //    return new AutoValue_PadLockEntry_WithPackageActivityName(PACKAGE_EMPTY, ACTIVITY_EMPTY, null,
  //        0, 0, false);
  //  }
  //
  //  @CheckResult public static boolean isEmpty(@NonNull WithPackageActivityName entry) {
  //    return PACKAGE_EMPTY.equals(entry.packageName()) && ACTIVITY_EMPTY.equals(
  //        entry.activityName());
  //  }
  //}

  @SuppressWarnings("WeakerAccess") public static class InsertManager {
    @NonNull private final Insert_entry insertEntry;

    InsertManager(@NonNull SQLiteOpenHelper openHelper) {
      this.insertEntry = new Insert_entry(openHelper.getWritableDatabase());
    }

    @CheckResult public long executeProgram(@NonNull PadLockEntry padLockEntry) {
      insertEntry.bind(padLockEntry.packageName(), padLockEntry.activityName(),
          padLockEntry.lockCode(), padLockEntry.lockUntilTime(), padLockEntry.ignoreUntilTime(),
          padLockEntry.systemApplication(), padLockEntry.whitelist());
      return insertEntry.program.executeInsert();
    }
  }

  @SuppressWarnings("WeakerAccess") public static class DeletePackageManager {
    @NonNull private final Delete_with_package_name deletePackage;

    DeletePackageManager(@NonNull SQLiteOpenHelper openHelper) {
      this.deletePackage = new Delete_with_package_name(openHelper.getWritableDatabase());
    }

    @CheckResult public int executeProgram(@NonNull String packageName) {
      deletePackage.bind(packageName);
      return deletePackage.program.executeUpdateDelete();
    }
  }

  @SuppressWarnings("WeakerAccess") public static class DeletePackageActivityManager {
    @NonNull private final Delete_with_package_activity_name deletePackageActivity;

    DeletePackageActivityManager(@NonNull SQLiteOpenHelper openHelper) {
      this.deletePackageActivity =
          new Delete_with_package_activity_name(openHelper.getWritableDatabase());
    }

    @CheckResult
    public int executeProgram(@NonNull String packageName, @NonNull String activityName) {
      deletePackageActivity.bind(packageName, activityName);
      return deletePackageActivity.program.executeUpdateDelete();
    }
  }

  @SuppressWarnings("WeakerAccess") public static class UpdateLockTimeManager {
    @NonNull private final Update_lock_until_time updateLockUntilTime;

    UpdateLockTimeManager(@NonNull SQLiteOpenHelper openHelper) {
      this.updateLockUntilTime = new Update_lock_until_time(openHelper.getWritableDatabase());
    }

    @CheckResult public int executeProgram(long time, @NonNull String packageName,
        @NonNull String activityName) {
      updateLockUntilTime.bind(time, packageName, activityName);
      return updateLockUntilTime.program.executeUpdateDelete();
    }
  }

  @SuppressWarnings("WeakerAccess") public static class UpdateIgnoreTimeManager {
    @NonNull private final Update_ignore_until_time updateIgnoreUntilTime;

    UpdateIgnoreTimeManager(@NonNull SQLiteOpenHelper openHelper) {
      this.updateIgnoreUntilTime = new Update_ignore_until_time(openHelper.getWritableDatabase());
    }

    @CheckResult public int executeProgram(long time, @NonNull String packageName,
        @NonNull String activityName) {
      updateIgnoreUntilTime.bind(time, packageName, activityName);
      return updateIgnoreUntilTime.program.executeUpdateDelete();
    }
  }

  @SuppressWarnings("WeakerAccess") public static class UpdateWhitelistManager {
    @NonNull private final Update_whitelist updateWhitelist;

    UpdateWhitelistManager(@NonNull SQLiteOpenHelper openHelper) {
      this.updateWhitelist = new Update_whitelist(openHelper.getWritableDatabase());
    }

    @CheckResult public int executeProgram(boolean whitelist, @NonNull String packageName,
        @NonNull String activityName) {
      updateWhitelist.bind(whitelist, packageName, activityName);
      return updateWhitelist.program.executeUpdateDelete();
    }
  }
}
