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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
              Timber.w("Entry: %s has no activities, hide.", info.packageName);
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

          final List<PadLockEntry.AllEntries> copyEntries = new ArrayList<>(padLockEntries);
          final List<String> copyNames = new ArrayList<>(packageNames);
          final List<LockTuple> lockTuples = new ArrayList<>();
          for (final String packageName : copyNames) {
            boolean locked = false;
            boolean whitelist = false;
            boolean hardLocked = false;
            final Set<PadLockEntry.AllEntries> removeEntries = new HashSet<>();
            for (final PadLockEntry.AllEntries entry : copyEntries) {
              if (entry.packageName().equals(packageName)) {
                removeEntries.add(entry);
                if (entry.activityName().equals(PadLockEntry.PACKAGE_ACTIVITY_NAME)) {
                  locked = true;
                } else if (entry.whitelist()) {
                  whitelist = true;
                } else {
                  hardLocked = true;
                }
              }
            }

            copyEntries.removeAll(removeEntries);
            lockTuples.add(new LockTuple(packageName, locked, whitelist, hardLocked));
          }

          return lockTuples;
        })
        .flatMapObservable(Observable::fromIterable)
        .flatMapMaybe(this::createFromPackageInfo)
        .toSortedList((entry, entry2) -> entry.name().compareToIgnoreCase(entry2.name()));
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Maybe<AppEntry> createFromPackageInfo(
      @NonNull LockTuple tuple) {
    return packageManagerWrapper.getApplicationInfo(tuple.packageName)
        .map(info -> AppEntry.builder()
            .name(packageManagerWrapper.loadPackageLabel(info).blockingGet())
            .packageName(tuple.packageName)
            .system(isSystemApplication(info))
            .locked(tuple.locked)
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

  private static final class LockTuple {

    @NonNull final String packageName;
    final boolean locked;
    final boolean whitelist;
    final boolean hardLocked;

    LockTuple(@NonNull String packageName, boolean locked, boolean whitelist, boolean hardLocked) {
      this.packageName = packageName;
      this.locked = locked;
      this.whitelist = whitelist;
      this.hardLocked = hardLocked;
    }
  }
}
