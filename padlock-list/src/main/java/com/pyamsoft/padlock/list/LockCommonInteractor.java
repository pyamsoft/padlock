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

package com.pyamsoft.padlock.list;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.model.LockState;
import io.reactivex.Single;
import timber.log.Timber;

abstract class LockCommonInteractor {

  @NonNull private final PadLockDB padLockDB;

  LockCommonInteractor(@NonNull PadLockDB padLockDB) {
    this.padLockDB = padLockDB;
  }

  @NonNull PadLockDB getPadLockDB() {
    return padLockDB;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Single<LockState> createNewEntry(
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, boolean whitelist) {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName);
    return getPadLockDB().insert(packageName, activityName, code, 0, 0, system, whitelist)
        .toSingleDefault(whitelist ? LockState.WHITELISTED : LockState.LOCKED);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Single<LockState> deleteEntry(
      @NonNull String packageName, @NonNull String activityName) {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName);
    return getPadLockDB().deleteWithPackageActivityName(packageName, activityName)
        .toSingleDefault(LockState.DEFAULT);
  }

  @NonNull @CheckResult
  public Single<LockState> modifySingleDatabaseEntry(@NonNull LockState oldLockState,
      @NonNull LockState newLockState, @NonNull String packageName, @NonNull String activityName,
      @Nullable String code, boolean system) {
    if (newLockState == LockState.WHITELISTED) {
      return Single.defer(() -> {
        final Single<LockState> newState;
        if (oldLockState == LockState.DEFAULT) {
          Timber.d("Add new as whitelisted");
          newState = createNewEntry(packageName, activityName, code, system, true);
        } else {
          // Update existing entry
          newState = Single.just(LockState.NONE);
        }
        return newState;
      });
    } else if (newLockState == LockState.LOCKED) {
      return Single.defer(() -> {
        final Single<LockState> newState;
        if (oldLockState == LockState.DEFAULT) {
          Timber.d("Add new as force locked");
          newState = createNewEntry(packageName, activityName, code, system, false);
        } else {
          // Update existing entry
          newState = Single.just(LockState.NONE);
        }
        return newState;
      });
    } else {
      return Single.defer(() -> {
        final Single<LockState> newState;
        if (oldLockState == LockState.DEFAULT) {
          Timber.d("Add new entry");
          newState = createNewEntry(packageName, activityName, code, system, false);
        } else {
          Timber.d("Delete existing entry");
          newState = deleteEntry(packageName, activityName);
        }
        return newState;
      });
    }
  }

  public abstract void clearCache();
}
