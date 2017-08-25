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

