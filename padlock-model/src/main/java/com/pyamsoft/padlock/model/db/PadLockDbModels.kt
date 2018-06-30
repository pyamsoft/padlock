package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

object PadLockDbModels {

  /**
   * The activity name of the PACKAGE entry in the database
   */
  const val PACKAGE_ACTIVITY_NAME = "PACKAGE"
  const val PACKAGE_EMPTY = "EMPTY"
  const val ACTIVITY_EMPTY = "EMPTY"

  @JvmStatic
  @CheckResult
  fun isEmpty(entry: PadLockEntryModel): Boolean = (PACKAGE_EMPTY == entry.packageName()
      && ACTIVITY_EMPTY == entry.activityName())

  @JvmField
  val EMPTY: PadLockEntryModel = object : PadLockEntryModel {
    override fun packageName(): String {
      return PACKAGE_EMPTY
    }

    override fun activityName(): String {
      return ACTIVITY_EMPTY
    }

    override fun lockCode(): String? {
      return null
    }

    override fun whitelist(): Boolean {
      return false
    }

    override fun systemApplication(): Boolean {
      return false
    }

    override fun ignoreUntilTime(): Long {
      return 0
    }

    override fun lockUntilTime(): Long {
      return 0
    }

  }

}
