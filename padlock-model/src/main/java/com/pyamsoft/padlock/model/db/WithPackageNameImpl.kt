package com.pyamsoft.padlock.model.db

import androidx.annotation.CheckResult

data class WithPackageNameImpl internal constructor(
  private val activityName: String,
  private val whitelist: Boolean
) : WithPackageNameModel {

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
      activityName: String,
      whitelist: Boolean
    ): WithPackageNameModel {
      return WithPackageNameImpl(activityName, whitelist)
    }

  }
}