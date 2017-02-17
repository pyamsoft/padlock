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
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.helper.Locker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockInfoInteractor extends LockCommonInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final Map<String, List<ActivityEntry>>
      activityEntryCache;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends Activity> lockScreenClass;
  @SuppressWarnings("WeakerAccess") @NonNull final Locker locker = Locker.newLock();
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockInfoInteractor(PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockPreferences preferences,
      @NonNull Class<? extends Activity> lockScreenClass) {
    super(padLockDB);
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
    this.lockScreenClass = lockScreenClass;
    activityEntryCache = new HashMap<>();
  }

  @NonNull @Override public Observable<LockState> modifySingleDatabaseEntry(boolean notInDatabase,
      @NonNull String packageName, @NonNull String activityName, @Nullable String code,
      boolean system, boolean whitelist, boolean forceLock) {
    return super.modifySingleDatabaseEntry(notInDatabase, packageName, activityName, code, system,
        whitelist, forceLock).flatMap(lockState -> {
      final Observable<LockState> resultState;
      if (lockState == LockState.NONE) {
        Timber.d("Not handled by modifySingleDatabaseEntry, entry must be updated");
        resultState = updateExistingEntry(packageName, activityName, whitelist);
      } else {
        resultState = Observable.just(lockState);
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
    });
  }

  @CheckResult @NonNull public Observable<Boolean> hasShownOnBoarding() {
    return Observable.fromCallable(preferences::isDialogOnBoard).delay(300, TimeUnit.MILLISECONDS);
  }

  public void clearCache() {
    activityEntryCache.clear();
  }

  public void updateCacheEntry(@NonNull String packageName, @NonNull String name,
      @NonNull LockState lockState) {
    final List<ActivityEntry> activityEntries = activityEntryCache.get(packageName);
    if (activityEntries == null) {
      Timber.e("No list of activities exists for %s, do not update", packageName);
      return;
    }

    final int size = activityEntries.size();
    for (int i = 0; i < size; ++i) {
      final ActivityEntry activityEntry = activityEntries.get(i);
      if (activityEntry.name().equals(name)) {
        Timber.d("Update cached entry: %s %s", name, lockState);
        activityEntries.set(i, ActivityEntry.builder(activityEntry).lockState(lockState).build());
      }
    }
  }

  @NonNull @CheckResult public Observable<ActivityEntry> populateList(@NonNull String packageName) {
    return Observable.defer(() -> {
      locker.waitForUnlock();
      Timber.d("populateList");
      final Observable<ActivityEntry> dataSource;
      if (isCacheEmpty()) {
        locker.prepareLock();
        dataSource = fetchFreshData(packageName).doOnTerminate(locker::unlock)
            .doOnUnsubscribe(locker::unlock);
      } else {
        dataSource = getCachedEntries(packageName);
      }
      return dataSource;
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult boolean isCacheEmpty() {
    return activityEntryCache.isEmpty();
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<ActivityEntry> getCachedEntries(@NonNull String packageName) {
    return Observable.fromCallable(() -> {
      List<ActivityEntry> activityEntries = activityEntryCache.get(packageName);
      if (activityEntries == null) {
        activityEntries = Collections.emptyList();
      }
      return activityEntries;
    }).flatMap(Observable::from);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull Observable<ActivityEntry> fetchFreshData(
      @NonNull String packageName) {
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
        }).flatMap(Observable::from).sorted((activityEntry, activityEntry2) -> {
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
    }).doOnNext(entry -> cacheEntry(packageName, entry));
  }

  @SuppressWarnings("WeakerAccess") void cacheEntry(@NonNull String packageName,
      @NonNull ActivityEntry entry) {
    List<ActivityEntry> list = activityEntryCache.get(packageName);
    if (list == null) {
      list = new ArrayList<>();
      activityEntryCache.put(packageName, list);
    }

    list.add(entry);
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<List<PadLockEntry.WithPackageName>> getLockedActivityEntries(
      @NonNull String packageName) {
    return getPadLockDB().queryWithPackageName(packageName).first();
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Observable<List<String>> getPackageActivities(@NonNull String packageName) {
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
