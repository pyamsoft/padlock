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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import rx.Observable;
import timber.log.Timber;

abstract class LockCommonInteractorImpl implements LockCommonInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final Context appContext;

  LockCommonInteractorImpl(final @NonNull Context context) {
    appContext = context.getApplicationContext();
  }

  @NonNull @CheckResult final Context getAppContext() {
    return appContext;
  }

  @NonNull @Override
  public Observable<LockState> modifySingleDatabaseEntry(@NonNull String packageName,
      @NonNull String activityName, @Nullable String code, boolean system, boolean whitelist,
      boolean forceLock) {
    final Observable<PadLockEntry> padLockEntryObservable =
        PadLockDB.with(appContext).queryWithPackageActivityName(packageName, activityName).first();

    if (whitelist) {
      Timber.d("Whitelist call");
      return padLockEntryObservable.flatMap(entry -> {
        if (PadLockEntry.isEmpty(entry)) {
          return createNewEntry(packageName, activityName, code, system, true);
        } else {
          return updateExistingEntry(entry, true);
        }
      });
    } else if (forceLock) {
      Timber.d("Force lock call");
      return padLockEntryObservable.flatMap(entry -> {
        if (PadLockEntry.isEmpty(entry)) {
          return createNewEntry(packageName, activityName, code, system, false);
        } else {
          return updateExistingEntry(entry, false);
        }
      });
    } else {
      Timber.d("Normal call");
      return padLockEntryObservable.flatMap(entry -> {
        if (PadLockEntry.isEmpty(entry)) {
          return createNewEntry(packageName, activityName, code, system, false);
        } else {
          return deleteEntry(packageName, activityName);
        }
      });
    }
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<LockState> createNewEntry(
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, boolean whitelist) {
    Timber.d("Empty entry, create a new entry for: %s %s", packageName, activityName);
    return PadLockDB.with(appContext)
        .insert(packageName, activityName, code, 0, 0, system, whitelist)
        .map(result -> {
          Timber.d("Insert result: %d", result);
          Timber.d("Whitelist: %s", whitelist);
          return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
        });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<LockState> updateExistingEntry(
      @NonNull PadLockEntry entry, boolean whitelist) {
    Timber.d("Entry already exists for: %s %s, update it", entry.packageName(),
        entry.activityName());
    return PadLockDB.with(appContext)
        .updateWithPackageActivityName(entry.packageName(), entry.activityName(), entry.lockCode(),
            entry.lockUntilTime(), entry.ignoreUntilTime(), entry.systemApplication(), whitelist)
        .map(result -> {
          Timber.d("Update result: %d", result);
          Timber.d("Whitelist: %s", whitelist);
          return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
        });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<LockState> deleteEntry(
      @NonNull String packageName, @NonNull String activityName) {
    Timber.d("Entry already exists for: %s %s, delete it", packageName, activityName);
    return PadLockDB.with(appContext)
        .deleteWithPackageActivityName(packageName, activityName)
        .map(result -> {
          Timber.d("Delete result: %d", result);
          return LockState.DEFAULT;
        });
  }
}
