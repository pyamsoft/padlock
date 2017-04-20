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

package com.pyamsoft.padlock.purge;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton public class PurgeInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") @Nullable Single<List<String>> cachedStalePackages;

  @Inject PurgeInteractor(@NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockDB padLockDB) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
  }

  public void clearCache() {
    cachedStalePackages = null;
  }

  /**
   * public
   */
  @NonNull Observable<String> populateList(boolean forceRefresh) {
    return Single.defer(() -> {
      final Single<List<String>> dataSource;
      if (cachedStalePackages == null || forceRefresh) {
        Timber.d("Refresh stale package");
        dataSource = fetchFreshData().cache();
        cachedStalePackages = dataSource;
      } else {
        Timber.d("Fetch stale from cache");
        dataSource = cachedStalePackages;
      }
      return dataSource;
    }).flatMapObservable(Observable::fromIterable).sorted(String::compareToIgnoreCase);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Single<List<String>> fetchFreshData() {
    return getAppEntryList().zipWith(getActiveApplicationPackageNames(),
        (allEntries, packageNames) -> {
          final List<String> stalePackageNames = new ArrayList<>();
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
        });
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Single<List<String>> getActiveApplicationPackageNames() {
    return packageManagerWrapper.getActiveApplications()
        .flatMapObservable(Observable::fromIterable)
        .map(applicationInfo -> applicationInfo.packageName)
        .toSortedList();
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Single<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return padLockDB.queryAll();
  }

  /**
   * public
   */
  @CheckResult @NonNull Completable deleteEntry(@NonNull String packageName) {
    return padLockDB.deleteWithPackageName(packageName)
        .andThen(Completable.fromAction(this::clearCache));
  }
}
