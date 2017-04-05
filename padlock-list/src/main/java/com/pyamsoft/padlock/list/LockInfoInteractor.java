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

import android.app.Activity;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.base.db.PadLockEntry;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton class LockInfoInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends Activity> lockScreenClass;
  @SuppressWarnings("WeakerAccess") @NonNull final LockInfoCacheInteractor cacheInteractor;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final PadLockDB padlockDB;

  @Inject LockInfoInteractor(@NonNull PadLockDB padLockDB,
      @NonNull LockInfoCacheInteractor cacheInteractor,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockPreferences preferences,
      @NonNull @Named("lockscreen") Class<? extends Activity> lockScreenClass) {
    this.padlockDB = padLockDB;
    this.cacheInteractor = cacheInteractor;
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
    this.lockScreenClass = lockScreenClass;
  }

  @CheckResult @NonNull public Observable<Boolean> hasShownOnBoarding() {
    return Observable.fromCallable(preferences::isDialogOnBoard).delay(300, TimeUnit.MILLISECONDS);
  }

  @NonNull @CheckResult
  public Observable<ActivityEntry> populateList(@NonNull String packageName, boolean forceRefresh) {
    return Single.defer(() -> {
      final Single<List<ActivityEntry>> dataSource;
      Single<List<ActivityEntry>> cached = cacheInteractor.getFromCache(packageName);
      if (cached == null || forceRefresh) {
        Timber.d("Refresh info list data");
        dataSource = fetchFreshData(packageName).cache();
        cacheInteractor.putIntoCache(packageName, dataSource);
      } else {
        Timber.d("Fetch info from cache");
        dataSource = cached;
      }
      return dataSource;
    }).toObservable().flatMap(Observable::fromIterable).sorted((activityEntry, activityEntry2) -> {
      // Package names are all the same
      final String entry1Name = activityEntry.name();
      final String entry2Name = activityEntry2.name();

      // Calculate if the starting X characters in the activity name is the exact package name
      boolean activity1Package = false;
      if (entry1Name.startsWith(packageName)) {
        final String strippedPackageName = entry1Name.replace(packageName, "");
        if (strippedPackageName.charAt(0) == '.') {
          activity1Package = true;
        }
      }

      boolean activity2Package = false;
      if (entry2Name.startsWith(packageName)) {
        final String strippedPackageName = entry2Name.replace(packageName, "");
        if (strippedPackageName.charAt(0) == '.') {
          activity2Package = true;
        }
      }
      if (activity1Package && activity2Package) {
        return entry1Name.compareToIgnoreCase(entry2Name);
      } else if (activity1Package) {
        return -1;
      } else if (activity2Package) {
        return 1;
      } else {
        return entry1Name.compareToIgnoreCase(entry2Name);
      }
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull
  Single<List<ActivityEntry>> fetchFreshData(@NonNull String packageName) {
    return getPackageActivities(packageName).zipWith(getLockedActivityEntries(packageName),
        (activityNames, padLockEntries) -> {
          // Sort here to avoid stream break
          // If the list is empty, the old flatMap call can hang, causing a list loading error
          // Sort here where we are guaranteed a list of some kind
          Collections.sort(padLockEntries,
              (o1, o2) -> o1.activityName().compareToIgnoreCase(o2.activityName()));

          final List<ActivityEntry> activityEntries = new ArrayList<>();

          int start = 0;
          int end = activityNames.size() - 1;

          while (start <= end) {
            // Find entry to compare against
            final ActivityEntry entry1 = findActivityEntry(activityNames, padLockEntries, start);
            activityEntries.add(entry1);

            if (start != end) {
              final ActivityEntry entry2 = findActivityEntry(activityNames, padLockEntries, end);
              activityEntries.add(entry2);
            }

            ++start;
            --end;
          }

          return activityEntries;
        });
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Single<List<PadLockEntry.WithPackageName>> getLockedActivityEntries(@NonNull String packageName) {
    return padlockDB.queryWithPackageName(packageName).first(Collections.emptyList());
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult Single<List<String>> getPackageActivities(
      @NonNull String packageName) {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .filter(activityEntry -> !activityEntry.equalsIgnoreCase(lockScreenClass.getName()))
        .toList();
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull ActivityEntry findActivityEntry(
      @NonNull List<String> activityNames,
      @NonNull List<PadLockEntry.WithPackageName> padLockEntries, int index) {
    final String activityName = activityNames.get(index);
    final PadLockEntry.WithPackageName foundEntry = findMatchingEntry(padLockEntries, activityName);
    return createActivityEntry(activityName, foundEntry);
  }

  @CheckResult @Nullable private PadLockEntry.WithPackageName findMatchingEntry(
      @NonNull List<PadLockEntry.WithPackageName> padLockEntries, @NonNull String activityName) {
    if (padLockEntries.isEmpty()) {
      return null;
    }

    // Select a pivot point
    final int middle = padLockEntries.size() / 2;
    final PadLockEntry.WithPackageName pivotPoint = padLockEntries.get(middle);

    // Compare to pivot
    int start;
    int end;
    PadLockEntry.WithPackageName foundEntry = null;
    if (pivotPoint.activityName().equals(activityName)) {
      // We are the pivot
      foundEntry = pivotPoint;
      start = 0;
      end = -1;
    } else if (activityName.compareToIgnoreCase(pivotPoint.activityName()) < 0) {
      //  We are before the pivot point
      start = 0;
      end = middle - 1;
    } else {
      // We are after the pivot point
      start = middle + 1;
      end = padLockEntries.size() - 1;
    }

    while (start <= end) {
      final PadLockEntry.WithPackageName checkEntry1 = padLockEntries.get(start++);
      final PadLockEntry.WithPackageName checkEntry2 = padLockEntries.get(end--);
      if (activityName.equals(checkEntry1.activityName())) {
        foundEntry = checkEntry1;
        break;
      } else if (activityName.equals(checkEntry2.activityName())) {
        foundEntry = checkEntry2;
        break;
      }
    }

    if (foundEntry != null) {
      padLockEntries.remove(foundEntry);
    }

    return foundEntry;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull ActivityEntry createActivityEntry(
      @NonNull String name, @Nullable PadLockEntry.WithPackageName foundEntry) {
    final LockState state;
    if (foundEntry == null) {
      state = LockState.DEFAULT;
    } else {
      if (foundEntry.whitelist()) {
        state = LockState.WHITELISTED;
      } else {
        state = LockState.LOCKED;
      }
    }

    return ActivityEntry.builder().name(name).lockState(state).build();
  }
}
