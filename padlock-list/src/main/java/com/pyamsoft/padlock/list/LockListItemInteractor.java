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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.AppEntry;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton public class LockListItemInteractor extends LockCommonInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final LockListCacheInteractor cacheInteractor;

  @Inject LockListItemInteractor(@NonNull PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull LockListCacheInteractor cacheInteractor) {
    super(padLockDB);
    this.packageManagerWrapper = packageManagerWrapper;
    this.cacheInteractor = cacheInteractor;
  }

  @NonNull @Override
  public Observable<LockState> modifySingleDatabaseEntry(@NonNull LockState oldLockState,
      @NonNull LockState newLockState, @NonNull String packageName, @NonNull String activityName,
      @Nullable String code, boolean system) {
    return super.modifySingleDatabaseEntry(oldLockState, newLockState, packageName, activityName,
        code, system).map(lockState -> {
      updateCacheEntry(packageManagerWrapper.loadPackageLabel(packageName).blockingFirst(),
          packageName, lockState == LockState.LOCKED);
      return lockState;
    });
  }

  @Override public void clearCache() {
    cacheInteractor.clearCache();
  }

  @SuppressWarnings("WeakerAccess") void updateCacheEntry(@NonNull String name,
      @NonNull String packageName, boolean newLockState) {
    Single<List<AppEntry>> cached = cacheInteractor.retrieve();
    if (cached != null) {
      cacheInteractor.cache(cached.map(appEntries -> {
        final int size = appEntries.size();
        for (int i = 0; i < size; ++i) {
          final AppEntry appEntry = appEntries.get(i);
          if (appEntry.name().equals(name) && appEntry.packageName().equals(packageName)) {
            Timber.d("Update cached entry: %s %s", name, packageName);
            appEntries.set(i, appEntry.toBuilder().locked(newLockState).build());
          }
        }
        return appEntries;
      }));
    }
  }
}
