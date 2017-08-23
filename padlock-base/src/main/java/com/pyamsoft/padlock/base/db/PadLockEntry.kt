/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.base.db

import android.database.sqlite.SQLiteOpenHelper
import android.support.annotation.CheckResult
import com.google.auto.value.AutoValue
import com.pyamsoft.padlock.service.db.PadLockEntryModel
import com.squareup.sqldelight.RowMapper
import com.squareup.sqldelight.SqlDelightStatement

@AutoValue abstract class PadLockEntry : PadLockEntryModel {

  @AutoValue abstract class AllEntries : PadLockEntryModel.All_entriesModel

  @AutoValue abstract class WithPackageName : PadLockEntryModel.With_package_nameModel

  class InsertManager internal constructor(openHelper: SQLiteOpenHelper) {

    private val insertEntry = PadLockEntryModel.Insert_entry(openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(padLockEntry: PadLockEntry): Long {
      insertEntry.bind(padLockEntry.packageName(), padLockEntry.activityName(),
          padLockEntry.lockCode(), padLockEntry.lockUntilTime(), padLockEntry.ignoreUntilTime(),
          padLockEntry.systemApplication(), padLockEntry.whitelist())
      return insertEntry.program.executeInsert()
    }
  }

  class DeletePackageManager internal constructor(openHelper: SQLiteOpenHelper) {

    private val deletePackage = PadLockEntryModel.Delete_with_package_name(
        openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(packageName: String): Int {
      deletePackage.bind(packageName)
      return deletePackage.program.executeUpdateDelete()
    }
  }

  class DeletePackageActivityManager internal constructor(openHelper: SQLiteOpenHelper) {

    private val deletePackageActivity = PadLockEntryModel.Delete_with_package_activity_name(
        openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(packageName: String, activityName: String): Int {
      deletePackageActivity.bind(packageName, activityName)
      return deletePackageActivity.program.executeUpdateDelete()
    }
  }

  class UpdateLockTimeManager internal constructor(openHelper: SQLiteOpenHelper) {

    private val updateLockUntilTime = PadLockEntryModel.Update_lock_until_time(
        openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(time: Long, packageName: String,
        activityName: String): Int {
      updateLockUntilTime.bind(time, packageName, activityName)
      return updateLockUntilTime.program.executeUpdateDelete()
    }
  }

  class UpdateIgnoreTimeManager internal constructor(openHelper: SQLiteOpenHelper) {

    private val updateIgnoreUntilTime = PadLockEntryModel.Update_ignore_until_time(
        openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(time: Long, packageName: String,
        activityName: String): Int {
      updateIgnoreUntilTime.bind(time, packageName, activityName)
      return updateIgnoreUntilTime.program.executeUpdateDelete()
    }
  }

  class UpdateWhitelistManager internal constructor(openHelper: SQLiteOpenHelper) {
    private val updateWhitelist = PadLockEntryModel.Update_whitelist(openHelper.writableDatabase)

    @CheckResult
    fun executeProgram(whitelist: Boolean, packageName: String,
        activityName: String): Int {
      updateWhitelist.bind(whitelist, packageName, activityName)
      return updateWhitelist.program.executeUpdateDelete()
    }
  }

  companion object {

    /**
     * The activity name of the PACKAGE entry in the database
     */
    const val PACKAGE_ACTIVITY_NAME = "PACKAGE"
    const internal val PACKAGE_EMPTY = "EMPTY"
    const internal val ACTIVITY_EMPTY = "EMPTY"

    @JvmStatic
    private var insertManager: InsertManager? = null
    @JvmStatic
    private var deletePackageManager: DeletePackageManager? = null
    @JvmStatic
    private var deletePackageActivityManager: DeletePackageActivityManager? = null
    @JvmStatic
    private var updateLockTimeManager: UpdateLockTimeManager? = null
    @JvmStatic
    private var updateIgnoreTimeManager: UpdateIgnoreTimeManager? = null
    @JvmStatic
    private var updateWhitelistManager: UpdateWhitelistManager? = null

    @JvmStatic
    val EMPTY: PadLockEntry by lazy {
      AutoValue_PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, 0, 0,
          false, false)
    }

    @JvmStatic
    private val FACTORY: PadLockEntryModel.Factory<PadLockEntry> by lazy<PadLockEntryModel.Factory<PadLockEntry>> {
      PadLockEntryModel.Factory { packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, systemApplication, whitelist ->
        AutoValue_PadLockEntry(packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime,
            systemApplication, whitelist)
      }
    }

    @JvmStatic
    internal val ALL_ENTRIES_MAPPER: RowMapper<AllEntries> by lazy<RowMapper<AllEntries>> {
      FACTORY.all_entriesMapper { packageName, activityName, whitelist ->
        AutoValue_PadLockEntry_AllEntries(packageName, activityName, whitelist)
      }
    }

    @JvmStatic
    internal val WITH_PACKAGE_NAME_MAPPER: RowMapper<WithPackageName> by lazy<RowMapper<WithPackageName>> {
      FACTORY.with_package_nameMapper { activityName, whitelist ->
        AutoValue_PadLockEntry_WithPackageName(activityName, whitelist)
      }
    }

    @JvmStatic
    internal val WITH_PACKAGE_ACTIVITY_NAME_DEFAULT_MAPPER: PadLockEntryModel.Mapper<PadLockEntry> by lazy {
      FACTORY.with_package_activity_name_defaultMapper()
    }

    @JvmStatic
    @CheckResult
    internal fun withPackageActivityNameDefault(packageName: String,
        activityName: String): SqlDelightStatement {
      return FACTORY.with_package_activity_name_default(packageName, PACKAGE_ACTIVITY_NAME,
          activityName, PACKAGE_ACTIVITY_NAME, activityName)
    }

    @JvmStatic
    @CheckResult internal fun withPackageName(packageName: String): SqlDelightStatement =
        FACTORY.with_package_name(packageName)

    @JvmStatic
    @CheckResult internal fun queryAll(): SqlDelightStatement = FACTORY.all_entries()

    @JvmStatic
    @CheckResult
    fun create(packageName: String, activityName: String,
        lockCode: String?, lockUntilTime: Long, ignoreUntilTime: Long, isSystem: Boolean,
        whitelist: Boolean): PadLockEntry {
      return FACTORY.creator.create(packageName, activityName, lockCode, lockUntilTime,
          ignoreUntilTime, isSystem, whitelist)
    }

    @JvmStatic
    @CheckResult
    fun isEmpty(entry: PadLockEntry): Boolean =
        PACKAGE_EMPTY == entry.packageName() && ACTIVITY_EMPTY == entry.activityName()

    @JvmStatic
    @CheckResult internal fun insertEntry(openHelper: SQLiteOpenHelper): InsertManager {
      val obj: InsertManager? = insertManager
      return if (obj == null) {
        val im = InsertManager(openHelper)
        insertManager = im

        // Return
        im
      } else {
        // Return
        obj
      }
    }

    @JvmStatic
    @CheckResult
    internal fun deletePackage(openHelper: SQLiteOpenHelper): DeletePackageManager {
      val obj: DeletePackageManager? = deletePackageManager
      return if (obj == null) {
        val dpm = DeletePackageManager(openHelper)
        deletePackageManager = dpm

        // Return
        dpm
      } else {
        // Return
        obj
      }
    }

    @JvmStatic
    @CheckResult
    internal fun deletePackageActivity(
        openHelper: SQLiteOpenHelper): DeletePackageActivityManager {
      val obj: DeletePackageActivityManager? = deletePackageActivityManager
      return if (obj == null) {
        val dpam = DeletePackageActivityManager(openHelper)
        deletePackageActivityManager = dpam

        // Return
        dpam
      } else {
        // Return
        obj
      }
    }

    @JvmStatic
    @CheckResult
    internal fun updateLockTime(openHelper: SQLiteOpenHelper): UpdateLockTimeManager {
      val obj: UpdateLockTimeManager? = updateLockTimeManager
      return if (obj == null) {
        val ultm = UpdateLockTimeManager(openHelper)
        updateLockTimeManager = ultm

        // Return
        ultm
      } else {
        // Return
        obj
      }
    }

    @JvmStatic
    @CheckResult
    internal fun updateIgnoreTime(openHelper: SQLiteOpenHelper): UpdateIgnoreTimeManager {
      val obj: UpdateIgnoreTimeManager? = updateIgnoreTimeManager
      return if (obj == null) {
        val uitm = UpdateIgnoreTimeManager(openHelper)
        updateIgnoreTimeManager = uitm
        // Return
        uitm
      } else {
        // Return
        obj
      }
    }

    @JvmStatic
    @CheckResult
    internal fun updateWhitelist(openHelper: SQLiteOpenHelper): UpdateWhitelistManager {
      val obj: UpdateWhitelistManager? = updateWhitelistManager
      return if (obj == null) {
        val uwm = UpdateWhitelistManager(openHelper)
        // Return
        updateWhitelistManager = uwm
        uwm
      } else {
        // Return
        obj
      }
    }

    @JvmStatic internal fun reset() {
      deletePackageManager = null
      deletePackageActivityManager = null
      insertManager = null
      updateLockTimeManager = null
      updateWhitelistManager = null
      updateIgnoreTimeManager = null
    }
  }
}
