package com.pyamsoft.padlock.base.database

import androidx.annotation.CheckResult
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockDbModels
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel

@Entity(
    tableName = PadLockEntryDb.TABLE_NAME,
    primaryKeys = [PadLockEntryDb.COLUMN_PACKAGE_NAME, PadLockEntryDb.COLUMN_ACTIVITY_NAME]
)
internal class PadLockEntryDb internal constructor(
) : PadLockEntryModel, AllEntriesModel, WithPackageNameModel {

  @field:ColumnInfo(name = COLUMN_PACKAGE_NAME)
  var packageName: String = PadLockDbModels.PACKAGE_EMPTY

  @field:ColumnInfo(name = COLUMN_ACTIVITY_NAME)
  var activityName: String = PadLockDbModels.ACTIVITY_EMPTY

  @field:ColumnInfo(name = COLUMN_LOCK_CODE)
  var lockCode: String? = null

  @field:ColumnInfo(name = COLUMN_LOCK_UNTIL_TIME)
  var lockUntilTime: Long? = null

  @field:ColumnInfo(name = COLUMN_IGNORE_UNTIL_TIME)
  var ignoreUntilTime: Long? = null

  @field:ColumnInfo(name = COLUMN_SYSTEM_APPLICATION)
  var systemApplication: Boolean? = null

  @field:ColumnInfo(name = COLUMN_WHITELIST)
  var whitelist: Boolean? = null

  @Ignore
  override fun packageName(): String {
    return packageName
  }

  @Ignore
  override fun activityName(): String {
    return activityName
  }

  @Ignore
  override fun lockCode(): String? {
    return lockCode
  }

  @Ignore
  override fun lockUntilTime(): Long {
    return lockUntilTime ?: 0
  }

  @Ignore
  override fun ignoreUntilTime(): Long {
    return ignoreUntilTime ?: 0
  }

  @Ignore
  override fun systemApplication(): Boolean {
    return systemApplication ?: false
  }

  @Ignore
  override fun whitelist(): Boolean {
    return whitelist ?: false
  }

  companion object {

    internal const val TABLE_NAME = "PadLockEntries"
    internal const val COLUMN_PACKAGE_NAME = "package_name"
    internal const val COLUMN_ACTIVITY_NAME = "activity_name"
    internal const val COLUMN_LOCK_CODE = "lock_code"
    internal const val COLUMN_LOCK_UNTIL_TIME = "lock_until_time"
    internal const val COLUMN_IGNORE_UNTIL_TIME = "ignore_until_time"
    internal const val COLUMN_SYSTEM_APPLICATION = "system_application"
    internal const val COLUMN_WHITELIST = "whitelist"

    @JvmStatic
    @CheckResult
    fun fromPadLockEntryModel(model: PadLockEntryModel): PadLockEntryDb {
      return PadLockEntryDb().also {
        it.packageName = model.packageName()
        it.activityName = model.activityName()
        it.lockCode = model.lockCode()
        it.lockUntilTime = model.lockUntilTime()
        it.ignoreUntilTime = model.ignoreUntilTime()
        it.systemApplication = model.systemApplication()
        it.whitelist = model.whitelist()
      }
    }
  }

}
