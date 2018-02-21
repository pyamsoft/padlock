/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.model

object Excludes {

  private val PACKAGES: List<String> = listOf(
      // Android system stuff. You can really shoot yourself in the foot here, don't let user
      // touch these packages
      "android",
      "com.android.systemui"
  )

  private val CLASSES: List<String> = listOf(
      // Lock Screen, can't lock itself
      "com.pyamsoft.padlock.lock.lockscreenactivity",

      // Leak Canary
      "com.squareup.leakcanary.internal.displayleakactivity",
      "com.squareup.leakcanary.internal.requeststoragepermissionactivity"
  )

  @JvmStatic
  fun isPackageExcluded(name: String): Boolean = checkExclusion(PACKAGES, name)

  @JvmStatic
  fun isClassExcluded(name: String): Boolean = checkExclusion(CLASSES, name)

  @JvmStatic
  private fun checkExclusion(
      list: List<String>,
      name: String
  ): Boolean {
    val check = name.trim()
        .toLowerCase()
    return list.contains(check)
  }

  @JvmStatic
  fun isLockScreen(
      packageName: String,
      className: String
  ): Boolean {
    val packageCheck = packageName.trim()
        .toLowerCase()
    val classCheck = className.trim()
        .toLowerCase()
    return (packageCheck == "com.pyamsoft.padlock"
        && classCheck == "com.pyamsoft.padlock.lock.lockscreenactivity")
  }
}
