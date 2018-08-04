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

package com.pyamsoft.padlock.lock.passed

import com.pyamsoft.padlock.api.lockscreen.LockPassed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockPassedImpl @Inject internal constructor() : LockPassed {

  private val passedSet: MutableCollection<String> = LinkedHashSet()

  override fun add(
    packageName: String,
    activityName: String
  ) {
    passedSet.add("$packageName$activityName")
  }

  override fun remove(
    packageName: String,
    activityName: String
  ) {
    passedSet.remove("$packageName$activityName")
  }

  override fun check(
    packageName: String,
    activityName: String
  ): Boolean =
    passedSet.contains("$packageName$activityName")
}
