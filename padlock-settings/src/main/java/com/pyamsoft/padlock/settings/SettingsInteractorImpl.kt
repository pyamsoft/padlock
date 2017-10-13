/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.settings

import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.pydroid.data.Cache
import com.pyamsoft.pydroid.helper.asOptional
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton internal class SettingsInteractorImpl @Inject internal constructor(
    private val deleteDb: PadLockDBDelete,
    private val masterPinPreference: MasterPinPreferences,
    private val preferences: ClearPreferences,
    private val installListenerPreferences: InstallListenerPreferences,
    @param:Named("cache_lock_list") private val lockListInteractor: Cache,
    @param:Named("cache_lock_info") private val lockInfoInteractor: Cache,
    @param:Named("cache_lock_entry") private val lockEntryInteractor: Cache,
    @param:Named("cache_purge") private val purgeInteractor: Cache) : SettingsInteractor {

  override fun isInstallListenerEnabled(): Single<Boolean> {
    return Single.fromCallable {
      installListenerPreferences.isInstallListenerEnabled()
    }
  }

  override fun clearDatabase(): Single<Boolean> {
    Timber.d("clear database")
    return deleteDb.deleteAll()
        .andThen(Completable.fromAction {
          lockListInteractor.clearCache()
          lockInfoInteractor.clearCache()
          purgeInteractor.clearCache()
          lockEntryInteractor.clearCache()
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
    return Single.fromCallable { masterPinPreference.getMasterPassword().asOptional() }
        .map { it.get() != null }
  }
}
