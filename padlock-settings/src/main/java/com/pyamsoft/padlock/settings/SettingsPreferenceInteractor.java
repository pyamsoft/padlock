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
import com.pyamsoft.padlock.base.preference.ClearPreferences;
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences;
import com.pyamsoft.padlock.base.preference.MasterPinPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.list.LockInfoItemInteractor;
import com.pyamsoft.padlock.list.LockListItemInteractor;
import com.pyamsoft.padlock.purge.PurgeInteractor;
import com.pyamsoft.pydroid.function.OptionalWrapper;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton class SettingsPreferenceInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final MasterPinPreferences masterPinPreference;
  @SuppressWarnings("WeakerAccess") @NonNull final ClearPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final InstallListenerPreferences
      installListenerPreferences;
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") @NonNull final LockListItemInteractor lockListInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoItemInteractor lockInfoInteractor;
  @SuppressWarnings("WeakerAccess") @NonNull final PurgeInteractor purgeInteractor;

  @Inject SettingsPreferenceInteractor(@NonNull PadLockDB padLockDB,
      @NonNull MasterPinPreferences masterPinPreference, @NonNull ClearPreferences preferences,
      @NonNull InstallListenerPreferences installListenerPreferences,
      @NonNull LockListItemInteractor lockListInteractor,
      @NonNull LockInfoItemInteractor lockInfoInteractor,
      @NonNull PurgeInteractor purgeInteractor) {
    this.padLockDB = padLockDB;
    this.masterPinPreference = masterPinPreference;
    this.preferences = preferences;
    this.installListenerPreferences = installListenerPreferences;
    this.lockListInteractor = lockListInteractor;
    this.lockInfoInteractor = lockInfoInteractor;
    this.purgeInteractor = purgeInteractor;
  }

  /**
   * public
   */
  @NonNull @CheckResult Observable<Boolean> isInstallListenerEnabled() {
    return Observable.fromCallable(installListenerPreferences::isInstallListenerEnabled);
  }

  /**
   * public
   */
  @NonNull @CheckResult Flowable<Boolean> clearDatabase() {
    return padLockDB.deleteAll().flatMap(result -> padLockDB.deleteDatabase()).map(aBoolean -> {
      lockListInteractor.clearCache();
      lockInfoInteractor.clearCache();
      purgeInteractor.clearCache();
      return Boolean.TRUE;
    });
  }

  /**
   * public
   */
  @NonNull @CheckResult Flowable<Boolean> clearAll() {
    return clearDatabase().map(aBoolean -> {
      Timber.d("Clear all preferences");
      preferences.clearAll();
      return Boolean.TRUE;
    });
  }

  /**
   * public
   */
  @CheckResult @NonNull Observable<Boolean> hasExistingMasterPassword() {
    return Observable.fromCallable(
        () -> OptionalWrapper.ofNullable(masterPinPreference.getMasterPassword()))
        .map(OptionalWrapper::isPresent);
  }
}
