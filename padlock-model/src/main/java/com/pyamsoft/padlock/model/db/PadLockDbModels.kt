package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.PadLockEntry

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
  val EMPTY: PadLockEntryModel =
    PadLockEntry(PACKAGE_EMPTY, ACTIVITY_EMPTY, null, false, false, 0, 0)

}
