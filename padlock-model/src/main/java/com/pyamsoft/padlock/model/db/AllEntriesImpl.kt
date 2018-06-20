package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

data class AllEntriesImpl internal constructor(
  private val packageName: String,
  private val activityName: String,
  private val whitelist: Boolean
) : AllEntriesModel {

  override fun packageName(): String {
    return packageName
  }

  override fun activityName(): String {
    return activityName
  }

  override fun whitelist(): Boolean {
    return whitelist
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun create(
      packageName: String,
      activityName: String,
      whitelist: Boolean
    ): AllEntriesModel {
      return AllEntriesImpl(packageName, activityName, whitelist)
    }
  }
}