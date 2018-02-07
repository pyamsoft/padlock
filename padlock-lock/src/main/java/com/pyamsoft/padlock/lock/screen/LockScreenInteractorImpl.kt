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

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.padlock.api.LockPassed
import com.pyamsoft.padlock.api.LockScreenInteractor
import com.pyamsoft.padlock.api.LockScreenPreferences
import com.pyamsoft.padlock.api.PackageLabelManager
import com.pyamsoft.padlock.model.LockScreenType
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockScreenInteractorImpl @Inject internal constructor(
    private val lockPassed: LockPassed,
    private val labelManager: PackageLabelManager,
    private val preferences: LockScreenPreferences
) :
    LockScreenInteractor {

  override fun getLockScreenType(): Single<LockScreenType> =
      Single.fromCallable { preferences.getCurrentLockType() }

  override fun getDefaultIgnoreTime(): Single<Long> =
      Single.fromCallable { preferences.getDefaultIgnoreTime() }

  override fun getDisplayName(packageName: String): Single<String> =
      labelManager.loadPackageLabel(packageName)

  override fun isAlreadyUnlocked(
      packageName: String,
      activityName: String
  ): Single<Boolean> =
      Single.fromCallable { lockPassed.check(packageName, activityName) }
}
