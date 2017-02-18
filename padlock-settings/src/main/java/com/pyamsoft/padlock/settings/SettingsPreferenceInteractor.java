/*
 * Copyright 2016 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.settings;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.list.LockInfoInteractor;
import com.pyamsoft.padlock.list.LockListInteractor;
import com.pyamsoft.padlock.purge.PurgeInteractor;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class SettingsPreferenceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockDB padLockDB;
  @NonNull final LockListInteractor lockListInteractor;
  @NonNull final LockInfoInteractor lockInfoInteractor;
  @NonNull final PurgeInteractor purgeInteractor;

  @Inject SettingsPreferenceInteractor(@NonNull PadLockDB padLockDB,
      @NonNull PadLockPreferences preferences, @NonNull LockListInteractor lockListInteractor,
      @NonNull LockInfoInteractor lockInfoInteractor, @NonNull PurgeInteractor purgeInteractor) {
    this.padLockDB = padLockDB;
    this.preferences = preferences;
    this.lockListInteractor = lockListInteractor;
    this.lockInfoInteractor = lockInfoInteractor;
    this.purgeInteractor = purgeInteractor;
  }

  @NonNull @CheckResult public Observable<Boolean> isInstallListenerEnabled() {
    return Observable.fromCallable(preferences::isInstallListenerEnabled);
  }

  @NonNull @CheckResult public Observable<Boolean> clearDatabase() {
    return padLockDB.deleteAll().map(deleteResult -> {
      Timber.d("Database is cleared: %s", deleteResult);
      padLockDB.deleteDatabase();

      // TODO just return something valid
      lockListInteractor.clearCached();
      lockInfoInteractor.clearCached();
      purgeInteractor.clearCached();
      return Boolean.TRUE;
    });
  }

  @NonNull @CheckResult public Observable<Boolean> clearAll() {
    return clearDatabase().map(aBoolean -> {
      Timber.d("Clear all preferences");
      preferences.clearAll();
      return Boolean.TRUE;
    });
  }
}
