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
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton public class LockInfoItemInteractor extends LockCommonInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoCacheInteractor cacheInteractor;

  @Inject LockInfoItemInteractor(@NonNull PadLockDB padLockDB,
      @NonNull LockInfoCacheInteractor cacheInteractor) {
    super(padLockDB);
    this.cacheInteractor = cacheInteractor;
  }

  @Override public void clearCache() {
    cacheInteractor.clearCache();
  }

  @NonNull @Override
  public Observable<LockState> modifySingleDatabaseEntry(@NonNull LockState oldLockState,
      @NonNull LockState newLockState, @NonNull String packageName, @NonNull String activityName,
      @Nullable String code, boolean system) {
    return super.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
        code, system).flatMap(newState -> {
      final Observable<LockState> resultState;
      if (newState == LockState.NONE) {
        Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated");
        resultState =
            updateExistingEntry(packageName, activityName, newLockState == LockState.WHITELISTED);
      } else {
        resultState = Observable.just(newState).map(lockState1 -> {
          updateCacheEntry(packageName, activityName, lockState1);
          return lockState1;
        });
      }
      return resultState;
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<LockState> updateExistingEntry(
      @NonNull String packageName, @NonNull String activityName, boolean whitelist) {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName);
    return getPadLockDB().updateWhitelist(whitelist, packageName, activityName).map(result -> {
      Timber.d("Update result: %d", result);
      Timber.d("Whitelist: %s", whitelist);
      return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
    }).map(lockState -> {
      updateCacheEntry(packageName, activityName, lockState);
      return lockState;
    });
  }

  @SuppressWarnings("WeakerAccess") void updateCacheEntry(@NonNull String packageName,
      @NonNull String name, @NonNull LockState lockState) {
    Single<List<ActivityEntry>> cached = cacheInteractor.getFromCache(packageName);
    if (cached != null) {
      Timber.d("Attempt update cached entry for: %s", packageName);
      cacheInteractor.putIntoCache(packageName, cached.map(activityEntries -> {
        final int size = activityEntries.size();
        for (int i = 0; i < size; ++i) {
          final ActivityEntry activityEntry = activityEntries.get(i);
          if (activityEntry.name().equals(name)) {
            Timber.d("Update cached entry: %s %s", name, lockState);
            activityEntries.set(i,
                ActivityEntry.builder().name(activityEntry.name()).lockState(lockState).build());
          }
        }
        return activityEntries;
      }));
    }
  }
}
