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

package com.pyamsoft.padlock.settings

import com.popinnow.android.repo.MultiRepo
import com.popinnow.android.repo.Repo
import com.pyamsoft.padlock.api.ApplicationInstallReceiver
import com.pyamsoft.padlock.api.SettingsInteractor
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.preferences.ClearPreferences
import com.pyamsoft.padlock.api.preferences.InstallListenerPreferences
import com.pyamsoft.padlock.api.preferences.MasterPinPreferences
import com.pyamsoft.padlock.model.ConfirmEvent
import com.pyamsoft.padlock.model.ConfirmEvent.ALL
import com.pyamsoft.padlock.model.ConfirmEvent.DATABASE
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.core.cache.Cache
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal class SettingsInteractorImpl @Inject internal constructor(
  private val enforcer: Enforcer,
  private val deleteDao: EntryDeleteDao,
  private val masterPinPreference: MasterPinPreferences,
  private val preferences: ClearPreferences,
  private val installListenerPreferences: InstallListenerPreferences,
  @param:Named("cache_lock_list") private val lockListInteractor: Cache,
  @param:Named("cache_lock_info") private val lockInfoInteractor: Cache,
  @param:Named("cache_lock_entry") private val lockEntryInteractor: Cache,
  @param:Named("cache_list_state") private val listStateCache: Cache,
  @param:Named("cache_purge") private val purgeInteractor: Cache,
  private val receiver: ApplicationInstallReceiver,

    // Just access these Repo instances for clearAll
  @param:Named("repo_lock_list") private val lockListRepo: Repo<List<AppEntry>>,
  @param:Named("repo_lock_info") private val lockInfoRepo: MultiRepo<List<ActivityEntry>>,
  @param:Named("repo_purge_list") private val purgeListRepo: Repo<List<String>>
) : SettingsInteractor {

  override fun updateApplicationReceiver(): Completable {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable installListenerPreferences.isInstallListenerEnabled()
    }
        .map {
          if (it) {
            receiver.register()
          } else {
            receiver.unregister()
          }
        }
        .ignoreElement()
  }

  override fun clearDatabase(): Single<ConfirmEvent> {
    return Single.defer {
      Timber.d("clear database")
      enforcer.assertNotOnMainThread()
      return@defer deleteDao.deleteAll()
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            lockListInteractor.clearCache()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            lockInfoInteractor.clearCache()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            purgeInteractor.clearCache()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            lockEntryInteractor.clearCache()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            listStateCache.clearCache()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            lockListRepo.cancel()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            purgeListRepo.cancel()
          })
          .andThen(Completable.fromAction {
            enforcer.assertNotOnMainThread()
            lockInfoRepo.cancel()
          })
          .toSingleDefault(DATABASE)
    }
  }

  override fun clearAll(): Single<ConfirmEvent> {
    // We map here to make sure the clear all is complete before stream continues
    return clearDatabase().map {
      enforcer.assertNotOnMainThread()
      Timber.d("Clear all preferences")
      preferences.clearAll()
      return@map ALL
    }
  }

  override fun hasExistingMasterPassword(): Single<Boolean> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable masterPinPreference.getMasterPassword()
          .asOptional()
    }
        .map { it is Present }
  }
}
