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
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import com.pyamsoft.padlock.base.preference.LockListPreferences;
import com.pyamsoft.padlock.base.preference.OnboardingPreferences;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.function.OptionalWrapper;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton class LockListInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final LockListPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final OnboardingPreferences onboardingPreferences;
  @SuppressWarnings("WeakerAccess") @NonNull final PackageManagerWrapper packageManagerWrapper;
  @SuppressWarnings("WeakerAccess") @NonNull final LockListCacheInteractor cacheInteractor;
  @NonNull private final PadLockDB padLockDB;

  @Inject LockListInteractor(@NonNull PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull OnboardingPreferences onboardingPreferences,
      @NonNull LockListPreferences preferences, @NonNull LockListCacheInteractor cacheInteractor) {
    this.padLockDB = padLockDB;
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
    this.cacheInteractor = cacheInteractor;
    this.onboardingPreferences = onboardingPreferences;
  }

  /**
   * public
   */
  @CheckResult @NonNull Observable<AppEntry> populateList(boolean forceRefresh) {
    return Single.defer(() -> {
      final Single<List<AppEntry>> dataSource;

      Single<List<AppEntry>> cache = cacheInteractor.retrieve();
      if (cache == null || forceRefresh) {
        Timber.d("Refresh into cache");
        dataSource = fetchFreshData().cache();
        cacheInteractor.cache(dataSource);
      } else {
        Timber.d("Fetch from cache");
        dataSource = cache;
      }
      return dataSource;
    }).toObservable().concatMap(Observable::fromIterable);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Single<List<AppEntry>> fetchFreshData() {
    return getActiveApplications().withLatestFrom(isSystemVisible().toObservable(),
        // We need to cast this?
        (BiFunction<ApplicationInfo, Boolean, OptionalWrapper<ApplicationInfo>>) (applicationInfo, systemVisible) -> {
          if (systemVisible) {
            // If system visible, we show all apps
            return OptionalWrapper.ofNullable(applicationInfo);
          } else {
            return isSystemApplication(applicationInfo) ? OptionalWrapper.ofNullable(null)
                : OptionalWrapper.ofNullable(applicationInfo);
          }
        })
        .filter(OptionalWrapper::isPresent)
        .flatMapSingle(wrapper -> {
          ApplicationInfo info = wrapper.item();
          return getActivityListForApplication(info).map(activityList -> {
            if (activityList.isEmpty()) {
              return "";
            } else {
              return info.packageName;
            }
          });
        })
        .filter(s -> !s.isEmpty())
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
        .toObservable()
        .flatMap(Observable::fromIterable)
        .flatMapMaybe(pair -> createFromPackageInfo(pair.first, pair.second))
        .toSortedList((entry, entry2) -> entry.name().compareToIgnoreCase(entry2.name()));
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<AppEntry> createFromPackageInfo(
      @NonNull String packageName, boolean locked) {
    return packageManagerWrapper.getApplicationInfo(packageName)
        .map(info -> AppEntry.builder()
            .name(packageManagerWrapper.loadPackageLabel(info).blockingGet())
            .packageName(packageName)
            .system(isSystemApplication(info))
            .locked(locked)
            .build());
  }

  @SuppressWarnings("WeakerAccess") @CheckResult boolean isSystemApplication(
      @NonNull ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @SuppressWarnings("WeakerAccess") @NonNull Observable<ApplicationInfo> getActiveApplications() {
    return packageManagerWrapper.getActiveApplications()
        .flatMapObservable(Observable::fromIterable);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Single<List<String>> getActivityListForApplication(@NonNull ApplicationInfo info) {
    return packageManagerWrapper.getActivityListForPackage(info.packageName);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Single<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return padLockDB.queryAll();
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

  /**
   * public
   */
  @CheckResult @NonNull Single<Boolean> hasShownOnBoarding() {
    return Single.fromCallable(onboardingPreferences::isListOnBoard);
  }

  /**
   * public
   */
  @CheckResult @NonNull Single<Boolean> isSystemVisible() {
    return Single.fromCallable(preferences::isSystemVisible);
  }

  /**
   * public
   */
  void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }
}
