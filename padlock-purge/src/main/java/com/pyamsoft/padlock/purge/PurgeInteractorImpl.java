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

package com.pyamsoft.padlock.purge;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class PurgeInteractorImpl implements PurgeInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull static final Object LOCK = new Object();
  @SuppressWarnings("WeakerAccess") @NonNull final List<String> stalePackageNameCache;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject PurgeInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockDB padLockDB) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
    stalePackageNameCache = new ArrayList<>();
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<String> getActiveApplicationPackageNames() {
    return packageManagerWrapper.getActiveApplications()
        .map(applicationInfo -> applicationInfo.packageName);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return padLockDB.queryAll().first();
  }

  @SuppressWarnings("WeakerAccess") void stopRefreshing() {
    synchronized (LOCK) {
      refreshing = false;
    }
  }

  @NonNull @Override public Observable<String> populateList() {
    return Observable.defer(() -> {
      synchronized (LOCK) {
        while (refreshing) {
          // Empty
          // TODO use wait once we figure out why stuff doesn't work when we use wait
        }

        refreshing = true;
      }

      Timber.d("populateList");
      final Observable<String> dataSource;
      if (isCacheEmpty()) {
        dataSource = fetchFreshData();
      } else {
        dataSource = getCachedEntries();
      }
      return dataSource;
    })
        .doOnUnsubscribe(this::stopRefreshing)
        .doOnCompleted(this::stopRefreshing)
        .doOnTerminate(this::stopRefreshing);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<String> fetchFreshData() {
    return getAppEntryList().zipWith(getActiveApplicationPackageNames().toList(),
        (allEntries, packageNames) -> {
          final Set<String> stalePackageNames = new HashSet<>();
          if (allEntries.isEmpty()) {
            Timber.e("Database does not have any AppEntry items");
            return stalePackageNames;
          }

          // Loop through all the package names that we are aware of on the device
          final Set<PadLockEntry.AllEntries> foundLocations = new HashSet<>();
          for (final String packageName : packageNames) {
            foundLocations.clear();

            //noinspection Convert2streamapi
            for (final PadLockEntry.AllEntries entry : allEntries) {
              // If an entry is found in the database remove it
              if (entry.packageName().equals(packageName)) {
                foundLocations.add(entry);
              }
            }

            allEntries.removeAll(foundLocations);
          }

          // The remaining entries in the database are stale
          //noinspection Convert2streamapi
          for (final PadLockEntry.AllEntries entry : allEntries) {
            stalePackageNames.add(entry.packageName());
          }

          return stalePackageNames;
        }).flatMap(Observable::from).sorted(String::compareToIgnoreCase).map(packageName -> {
      cacheEntry(packageName);
      return packageName;
    });
  }

  @NonNull @Override public Observable<Integer> deleteEntry(@NonNull String packageName) {
    return padLockDB.deleteWithPackageName(packageName);
  }

  @Override public boolean isCacheEmpty() {
    return stalePackageNameCache.isEmpty();
  }

  @NonNull @CheckResult Observable<String> getCachedEntries() {
    return Observable.fromCallable(() -> stalePackageNameCache).flatMap(Observable::from);
  }

  @Override public void clearCache() {
    stalePackageNameCache.clear();
  }

  void cacheEntry(@NonNull String entry) {
    stalePackageNameCache.add(entry);
  }

  @Override public void removeFromCache(@NonNull String entry) {
    stalePackageNameCache.remove(entry);
  }
}
