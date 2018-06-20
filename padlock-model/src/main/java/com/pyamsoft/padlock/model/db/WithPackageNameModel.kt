package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

interface WithPackageNameModel {

  @CheckResult
  fun activityName(): String

  @CheckResult
  fun whitelist(): Boolean
}