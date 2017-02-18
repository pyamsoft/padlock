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
import android.support.annotation.Nullable;
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

public class PurgeInteractor {

  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padLockDB;
  @SuppressWarnings("WeakerAccess") @Nullable Observable<List<String>> cachedStalePackages;

  @Inject PurgeInteractor(@NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockDB padLockDB) {
    this.packageManagerWrapper = packageManagerWrapper;
    this.padLockDB = padLockDB;
  }

  public void clearCached() {
    cachedStalePackages = null;
  }

  @NonNull public Observable<String> populateList(boolean forceRefresh) {
    return Observable.defer(() -> {
      final Observable<List<String>> dataSource;
      if (cachedStalePackages == null || forceRefresh) {
        Timber.d("Refresh stale package");
        dataSource = fetchFreshData().cache();
        cachedStalePackages = dataSource;
      } else {
        Timber.d("Fetch stale from cache");
        dataSource = cachedStalePackages;
      }
      return dataSource;
    }).flatMap(Observable::from).sorted(String::compareToIgnoreCase);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Observable<List<String>> fetchFreshData() {
    return getAppEntryList().zipWith(getActiveApplicationPackageNames().toList(),
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
  Observable<String> getActiveApplicationPackageNames() {
    return packageManagerWrapper.getActiveApplications()
        .map(applicationInfo -> applicationInfo.packageName);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return padLockDB.queryAll().first();
  }

  @CheckResult @NonNull public Observable<Integer> deleteEntry(@NonNull String packageName) {
    return padLockDB.deleteWithPackageName(packageName).map(integer -> {
      cachedStalePackages = null;
      return integer;
    });
  }
}
