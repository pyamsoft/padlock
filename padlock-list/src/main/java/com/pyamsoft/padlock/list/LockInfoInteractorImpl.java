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
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.PadLockPreferences;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockInfoInteractorImpl extends LockCommonInteractorImpl implements LockInfoInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final List<ActivityEntry> activityEntryCache;
  @SuppressWarnings("WeakerAccess") @NonNull final Class<? extends Activity> lockScreenClass;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockInfoInteractorImpl(PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull PadLockPreferences preferences,
      @NonNull Class<? extends Activity> lockScreenClass) {
    super(padLockDB);
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
    this.lockScreenClass = lockScreenClass;
    activityEntryCache = new ArrayList<>();
  }

  @NonNull @Override public Observable<List<PadLockEntry.WithPackageName>> getLockedActivityEntries(
      @NonNull String packageName) {
    return getPadLockDB().queryWithPackageName(packageName).first();
  }

  @NonNull @Override
  public Observable<List<String>> getPackageActivities(@NonNull String packageName) {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .filter(activityEntry -> !activityEntry.equalsIgnoreCase(lockScreenClass.getName()))
        .toList();
  }

  @NonNull @Override public Observable<Boolean> hasShownOnBoarding() {
    return Observable.fromCallable(preferences::isLockInfoDialogOnBoard);
  }

  @Override public boolean isCacheEmpty() {
    return activityEntryCache.isEmpty();
  }

  @NonNull @Override public Observable<ActivityEntry> getCachedEntries() {
    return Observable.fromCallable(() -> activityEntryCache).flatMap(Observable::from);
  }

  @Override public void clearCache() {
    activityEntryCache.clear();
  }

  @Override public void updateCacheEntry(@NonNull String name, @NonNull LockState lockState) {
    final int size = activityEntryCache.size();
    for (int i = 0; i < size; ++i) {
      final ActivityEntry activityEntry = activityEntryCache.get(i);
      if (activityEntry.name().equals(name)) {
        Timber.d("Update cached entry: %s %s", name, lockState);
        activityEntryCache.set(i,
            ActivityEntry.builder(activityEntry).lockState(lockState).build());
      }
    }
  }

  @Override public void cacheEntry(@NonNull ActivityEntry entry) {
    activityEntryCache.add(entry);
  }

  @Override @NonNull public Observable<LockState> updateExistingEntry(@NonNull String packageName,
      @NonNull String activityName, boolean whitelist) {
    Timber.d("Entry already exists for: %s %s, update it", packageName, activityName);
    return getPadLockDB().updateWhitelist(whitelist, packageName, activityName).map(result -> {
      Timber.d("Update result: %d", result);
      Timber.d("Whitelist: %s", whitelist);
      return whitelist ? LockState.WHITELISTED : LockState.LOCKED;
    });
  }
}
