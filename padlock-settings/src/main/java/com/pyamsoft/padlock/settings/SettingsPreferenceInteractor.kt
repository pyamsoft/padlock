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

package com.pyamsoft.padlock.settings

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDB
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.padlock.list.LockInfoItemInteractor
import com.pyamsoft.padlock.list.LockListItemInteractor
import com.pyamsoft.padlock.purge.PurgeInteractor
import com.pyamsoft.pydroid.helper.Optional
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class SettingsPreferenceInteractor @Inject internal constructor(
    val padLockDB: PadLockDB,
    val masterPinPreference: MasterPinPreferences,
    val preferences: ClearPreferences,
    val installListenerPreferences: InstallListenerPreferences,
    val lockListInteractor: LockListItemInteractor,
    val lockInfoInteractor: LockInfoItemInteractor,
    val purgeInteractor: PurgeInteractor) {

  val isInstallListenerEnabled: Single<Boolean>
    @CheckResult get() = Single.fromCallable<Boolean> {
      installListenerPreferences.isInstallListenerEnabled
    }

  @CheckResult fun clearDatabase(): Single<Boolean> {
    return padLockDB.deleteAll()
        .andThen(padLockDB.deleteDatabase())
        .andThen(Completable.fromAction {
          lockListInteractor.clearCache()
        })
        .andThen(Completable.fromAction {
          lockInfoInteractor.clearCache()
        })
        .andThen(Completable.fromAction {
          purgeInteractor.clearCache()
        })
        .toSingleDefault(true)
  }

  @CheckResult fun clearAll(): Single<Boolean> {
    return clearDatabase().map {
      Timber.d("Clear all preferences")
      preferences.clearAll()
      return@map true
    }
  }

  @CheckResult fun hasExistingMasterPassword(): Single<Boolean> {
    return Single.fromCallable { Optional.ofNullable(masterPinPreference.masterPassword) }
        .map { it.isPresent() }
  }
}
