package com.pyamsoft.padlock.api

import androidx.annotation.CheckResult
import io.reactivex.Completable

interface EntryUpdateDao {

  @CheckResult
  fun updateLockUntilTime(
    packageName: String,
    activityName: String,
    lockUntilTime: Long
  ): Completable

  @CheckResult
  fun updateIgnoreUntilTime(
    packageName: String,
    activityName: String,
    ignoreUntilTime: Long
  ): Completable

  @CheckResult
  fun updateWhitelist(
    packageName: String,
    activityName: String,
    whitelist: Boolean
  ): Completable
}
