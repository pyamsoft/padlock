package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

interface AllEntriesModel {

  @CheckResult
  fun packageName(): String

  @CheckResult
  fun activityName(): String

  @CheckResult
  fun whitelist(): Boolean
}