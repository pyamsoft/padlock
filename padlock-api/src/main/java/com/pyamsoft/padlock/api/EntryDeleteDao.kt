package com.pyamsoft.padlock.api

import androidx.annotation.CheckResult
import io.reactivex.Completable

interface EntryDeleteDao {

  @CheckResult
  fun deleteWithPackageName(packageName: String): Completable

  @CheckResult
  fun deleteWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Completable

  @CheckResult
  fun deleteAll(): Completable
}
