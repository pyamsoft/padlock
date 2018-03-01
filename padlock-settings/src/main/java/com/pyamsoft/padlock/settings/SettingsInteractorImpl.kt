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

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.api.*
import com.pyamsoft.pydroid.cache.Cache
import com.pyamsoft.pydroid.optional.Optional.Present
import com.pyamsoft.pydroid.optional.asOptional
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class SettingsInteractorImpl @Inject internal constructor(
    private val deleteDb: PadLockDBDelete,
    private val masterPinPreference: MasterPinPreferences,
    private val preferences: ClearPreferences,
    private val installListenerPreferences: InstallListenerPreferences,
    @param:Named("cache_lock_list") private val lockListInteractor: Cache,
    @param:Named("cache_lock_info") private val lockInfoInteractor: Cache,
    @param:Named("cache_lock_entry") private val lockEntryInteractor: Cache,
    @param:Named("cache_app_icons") private val iconCache: Cache,
    @param:Named("cache_list_state") private val listStateCache: Cache,
    @param:Named("cache_purge") private val purgeInteractor: Cache
) :
    SettingsInteractor {

  override fun isInstallListenerEnabled(): Single<Boolean> =
      Single.fromCallable { installListenerPreferences.isInstallListenerEnabled() }

  override fun clearDatabase(): Single<Boolean> {
    Timber.d("clear database")
    return deleteDb.deleteAll()
        .andThen(Completable.fromAction {
          lockListInteractor.clearCache()
          lockInfoInteractor.clearCache()
          purgeInteractor.clearCache()
          lockEntryInteractor.clearCache()
          iconCache.clearCache()
          listStateCache.clearCache()
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
    return Single.fromCallable {
      masterPinPreference.getMasterPassword()
          .asOptional()
    }
        .map { it is Present }
  }
}
