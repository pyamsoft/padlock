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

import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.helper.Optional
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class SettingsInteractorImpl @Inject internal constructor(
    private val deleteDb: PadLockDBDelete,
    private val masterPinPreference: MasterPinPreferences,
    private val preferences: ClearPreferences,
    private val installListenerPreferences: InstallListenerPreferences,
    private val lockListInteractor: Cache,
    private val lockInfoInteractor: Cache,
    private val purgeInteractor: Cache) : SettingsInteractor {

  override fun isInstallListenerEnabled(): Single<Boolean> {
    return Single.fromCallable {
      installListenerPreferences.isInstallListenerEnabled()
    }
  }

  override fun clearDatabase(): Single<Boolean> {
    return deleteDb.deleteAll()
        .andThen(Completable.fromAction {
          lockListInteractor.clearCache()
          lockInfoInteractor.clearCache()
          purgeInteractor.clearCache()
        })
        .toSingleDefault(true)
  }

  override fun clearAll(): Single<Boolean> {
    // We map here to make sure the clear all is complete before stream continues
    return clearDatabase().map {
      Timber.d("Clear all preferences")
      preferences.clearAll()
      return@map true
    }
  }

  override fun hasExistingMasterPassword(): Single<Boolean> {
    return Single.fromCallable { Optional.ofNullable(masterPinPreference.getMasterPassword()) }
        .map { it.isPresent() }
  }
}
