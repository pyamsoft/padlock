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

import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockListInteractorImpl extends LockCommonInteractorImpl implements LockListInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull static final Object LOCK = new Object();
  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") @NonNull final List<AppEntry> appEntryCache;
  @SuppressWarnings("WeakerAccess") boolean refreshing;

  @Inject LockListInteractorImpl(PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockPreferences preferences) {
    super(padLockDB);
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
    appEntryCache = new ArrayList<>();
  }

  @SuppressWarnings("WeakerAccess") void stopRefreshing() {
    synchronized (LOCK) {
      refreshing = false;
    }
  }

  @NonNull @Override public Observable<AppEntry> populateList() {
    return Observable.defer(() -> {
      synchronized (LOCK) {
        while (refreshing) {
          // Empty
          // TODO use wait once we figure out why stuff doesn't work when we use wait
        }

        refreshing = true;
      }

      Timber.d("populateList");
      final Observable<AppEntry> dataSource;
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

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<AppEntry> fetchFreshData() {
    return getActiveApplications().withLatestFrom(isSystemVisible(),
        (applicationInfo, systemVisible) -> {
          if (systemVisible) {
            // If system visible, we show all apps
            return applicationInfo;
          } else {
            if (isSystemApplication(applicationInfo)) {
              // Application is system but system apps are hidden
              Timber.w("Hide system application: %s", applicationInfo.packageName);
              return null;
            } else {
              return applicationInfo;
            }
          }
        })
        .filter(applicationInfo -> applicationInfo != null)
        .flatMap(applicationInfo -> getActivityListForApplication(applicationInfo).toList()
            .map(activityList -> {
              if (activityList.isEmpty()) {
                Timber.w("Exclude package %s because it has no activities",
                    applicationInfo.packageName);
                return null;
              } else {
                return applicationInfo.packageName;
              }
            }))
        .filter(s -> s != null)
        .toList()
        .zipWith(getAppEntryList(), (packageNames, padLockEntries) -> {
          // Sort here to avoid stream break
          // If the list is empty, the old flatMap call can hang, causing a list loading error
          // Sort here where we are guaranteed a list of some kind
          Collections.sort(padLockEntries,
              (o1, o2) -> o1.packageName().compareToIgnoreCase(o2.packageName()));

          final List<Pair<String, Boolean>> lockPairs = new ArrayList<>();
          int start = 0;
          int end = packageNames.size() - 1;

          while (start <= end) {
            // Find entry to compare against
            final Pair<String, Boolean> entry1 = findAppEntry(packageNames, padLockEntries, start);
            lockPairs.add(entry1);

            if (start != end) {
              final Pair<String, Boolean> entry2 = findAppEntry(packageNames, padLockEntries, end);
              lockPairs.add(entry2);
            }

            ++start;
            --end;
          }

          return lockPairs;
        })
        .flatMap(Observable::from)
        .flatMap(pair -> createFromPackageInfo(pair.first, pair.second))
        .sorted((entry, entry2) -> entry.name().compareToIgnoreCase(entry2.name()))
        .doOnNext(this::cacheEntry);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Pair<String, Boolean> findAppEntry(
      @NonNull List<String> packageNames, @NonNull List<PadLockEntry.AllEntries> padLockEntries,
      int index) {
    final String packageName = packageNames.get(index);
    final PadLockEntry.AllEntries foundEntry = findMatchingEntry(padLockEntries, packageName);
    return new Pair<>(packageName, foundEntry != null);
  }

  @CheckResult @Nullable private PadLockEntry.AllEntries findMatchingEntry(
      @NonNull List<PadLockEntry.AllEntries> padLockEntries, @NonNull String packageName) {
    if (padLockEntries.isEmpty()) {
      return null;
    }

    // Select a pivot point
    final int middle = padLockEntries.size() / 2;
    final PadLockEntry.AllEntries pivotPoint = padLockEntries.get(middle);

    // Compare to pivot
    int start;
    int end;
    PadLockEntry.AllEntries foundEntry = null;
    if (pivotPoint.packageName().equals(packageName)) {
      // We are the pivot
      foundEntry = pivotPoint;
      start = 0;
      end = -1;
    } else if (packageName.compareToIgnoreCase(pivotPoint.packageName()) < 0) {
      //  We are before the pivot point
      start = 0;
      end = middle - 1;
    } else {
      // We are after the pivot point
      start = middle + 1;
      end = padLockEntries.size() - 1;
    }

    while (start <= end) {
      final PadLockEntry.AllEntries checkEntry1 = padLockEntries.get(start++);
      final PadLockEntry.AllEntries checkEntry2 = padLockEntries.get(end--);
      if (packageName.equals(checkEntry1.packageName())) {
        foundEntry = checkEntry1;
        break;
      } else if (packageName.equals(checkEntry2.packageName())) {
        foundEntry = checkEntry2;
        break;
      }
    }

    if (foundEntry != null) {
      padLockEntries.remove(foundEntry);
    }

    return foundEntry;
  }

  @NonNull @Override public Observable<Boolean> hasShownOnBoarding() {
    return Observable.fromCallable(preferences::isListOnBoard);
  }

  @NonNull @Override public Observable<Boolean> isSystemVisible() {
    return Observable.fromCallable(preferences::isSystemVisible);
  }

  @Override public void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }

  @SuppressWarnings("WeakerAccess") @NonNull Observable<ApplicationInfo> getActiveApplications() {
    return packageManagerWrapper.getActiveApplications();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Observable<String> getActivityListForApplication(@NonNull ApplicationInfo info) {
    return packageManagerWrapper.getActivityListForPackage(info.packageName);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Observable<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return getPadLockDB().queryAll().first();
  }

  @Override @CheckResult public boolean isSystemApplication(@NonNull ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult boolean isCacheEmpty() {
    return appEntryCache.isEmpty();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<AppEntry> getCachedEntries() {
    return Observable.fromCallable(() -> appEntryCache).concatMap(Observable::from);
  }

  @Override public void clearCache() {
    appEntryCache.clear();
  }

  @Override public void updateCacheEntry(@NonNull String name, @NonNull String packageName,
      boolean newLockState) {
    final int size = appEntryCache.size();
    for (int i = 0; i < size; ++i) {
      final AppEntry appEntry = appEntryCache.get(i);
      if (appEntry.name().equals(name) && appEntry.packageName().equals(packageName)) {
        Timber.d("Update cached entry: %s %s", name, packageName);
        appEntryCache.set(i, AppEntry.builder(appEntry).locked(newLockState).build());
      }
    }
  }

  @SuppressWarnings("WeakerAccess") void cacheEntry(@NonNull AppEntry entry) {
    appEntryCache.add(entry);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<AppEntry> createFromPackageInfo(@NonNull String packageName, boolean locked) {
    return packageManagerWrapper.getApplicationInfo(packageName)
        .map(info -> AppEntry.builder()
            .name(packageManagerWrapper.loadPackageLabel(info).toBlocking().first())
            .packageName(packageName)
            .system(isSystemApplication(info))
            .locked(locked)
            .build());
  }
}
