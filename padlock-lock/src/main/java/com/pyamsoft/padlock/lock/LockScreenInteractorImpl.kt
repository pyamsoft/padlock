/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.lock

import com.pyamsoft.padlock.api.lockscreen.LockPassed
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.preferences.LockScreenPreferences
import com.pyamsoft.padlock.model.LockScreenType
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

internal class LockScreenInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val lockPassed: LockPassed,
  private val labelManager: PackageLabelManager,
  private val preferences: LockScreenPreferences
) : LockScreenInteractor {

  override fun getLockScreenType(): Single<LockScreenType> =
    Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.getCurrentLockType()
    }

  override fun getDefaultIgnoreTime(): Single<Long> =
    Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable preferences.getDefaultIgnoreTime()
    }

  override fun getDisplayName(packageName: String): Single<String> = Single.defer {
    enforcer.assertNotOnMainThread()
    return@defer labelManager.loadPackageLabel(packageName)
  }

  override fun isAlreadyUnlocked(
    packageName: String,
    activityName: String
  ): Single<Boolean> = Single.fromCallable {
    enforcer.assertNotOnMainThread()
    return@fromCallable lockPassed.check(packageName, activityName)
  }
}
