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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.dagger.PadLockDB;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import rx.Observable;
import timber.log.Timber;

abstract class LockCommonInteractorImpl implements LockCommonInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockDB padLockDB;

  LockCommonInteractorImpl(@NonNull PadLockDB padLockDB) {
    this.padLockDB = padLockDB;
  }

  @NonNull PadLockDB getPadLockDB() {
    return padLockDB;
  }

  @Override @NonNull public Observable<LockState> createNewEntry(@NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist) {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName);
    return padLockDB.insert(packageName, activityName, code, 0, 0, system, whitelist)
        .map(result -> {
          Timber.d("Insert result: %d", result);
          Timber.d("Whitelist: %s", whitelist);
          return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
        });
  }

  @Override @NonNull public Observable<LockState> updateExistingEntry(@NonNull String packageName,
      @NonNull String activityName, boolean whitelist) {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName);
    return padLockDB.queryWithPackageActivityName(packageName, activityName)
        .first()
        .flatMap(entry -> {
          if (PadLockEntry.WithPackageActivityName.isEmpty(entry)) {
            throw new RuntimeException("PadLock entry is empty but update was called");
          }

          return padLockDB.updateWhitelist(whitelist, entry.packageName(), entry.activityName())
              .map(result -> {
                Timber.d("Update result: %d", result);
                Timber.d("Whitelist: %s", whitelist);
                return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
              });
        });
  }

  @Override @NonNull public Observable<LockState> deleteEntry(@NonNull String packageName,
      @NonNull String activityName) {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName);
    return padLockDB.deleteWithPackageActivityName(packageName, activityName).map(result -> {
      Timber.d("Delete result: %d", result);
      return LockState.DEFAULT;
    });
  }
}
