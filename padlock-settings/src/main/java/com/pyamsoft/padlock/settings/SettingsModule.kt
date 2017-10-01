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

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.preference.ClearPreferences
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences
import com.pyamsoft.padlock.base.preference.MasterPinPreferences
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class SettingsModule {

  @Singleton
  @Provides
  @CheckResult internal fun provideConfirmBus(): EventBus<ConfirmEvent> =
      ConfirmEventBus()

  @Singleton
  @Provides
  @CheckResult internal fun provideSettingsInteractor(
      deleteDb: PadLockDBDelete, masterPinPreference: MasterPinPreferences,
      clearPreference: ClearPreferences, installListenerPreferences: InstallListenerPreferences,
      @Named("cache_lock_list") lockListInteractor: Cache,
      @Named("cache_lock_info") lockInfoInteractor: Cache,
      @Named("cache_lock_entry") lockEntryInteractor: Cache,
      @Named("cache_purge") purgeInteractor: Cache): SettingsInteractor {
    return SettingsInteractorImpl(deleteDb, masterPinPreference, clearPreference,
        installListenerPreferences, lockListInteractor, lockInfoInteractor, lockEntryInteractor,
        purgeInteractor)
  }
}

