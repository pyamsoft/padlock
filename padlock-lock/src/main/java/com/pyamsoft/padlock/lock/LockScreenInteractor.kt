/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.lock

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.preference.LockScreenPreferences
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import com.pyamsoft.padlock.lock.common.LockTypeInteractor
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class LockScreenInteractor @Inject internal constructor(
    preferences: LockScreenPreferences,
    private val packageManagerWrapper: PackageManagerWrapper) : LockTypeInteractor(preferences) {

  @CheckResult fun getDefaultIgnoreTime(): Single<Long> {
    return Single.fromCallable { preferences.getDefaultIgnoreTime() }
  }

  @CheckResult fun getDisplayName(packageName: String): Single<String> {
    return packageManagerWrapper.loadPackageLabel(packageName).toSingle("")
  }
}