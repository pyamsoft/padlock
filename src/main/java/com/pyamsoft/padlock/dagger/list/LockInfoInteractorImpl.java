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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockInfoInteractorImpl extends LockCommonInteractorImpl implements LockInfoInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject LockInfoInteractorImpl(PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockPreferences preferences) {
    super(padLockDB);
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
  }

  @NonNull @Override public Observable<List<PadLockEntry.WithPackageName>> getActivityEntries(
      @NonNull String packageName) {
    return getPadLockDB().queryWithPackageName(packageName).first();
  }

  @NonNull @Override public Observable<String> getPackageActivities(@NonNull String packageName) {
    return packageManagerWrapper.getActivityListForPackage(packageName)
        .filter(
            activityEntry -> !activityEntry.equalsIgnoreCase(LockScreenActivity.class.getName()));
  }

  @NonNull @Override
  public Observable<Boolean> modifyDatabaseGroup(boolean allCreate, @NonNull String packageName,
      @Nullable String code, boolean system) {
    return getPackageActivities(packageName).flatMap(activityName -> {
      if (allCreate) {
        Timber.d("Modify the database to create entry for: %s %s", packageName, activityName);

        // No whitelisting for the package grouping, its an all or nothing
        return getPadLockDB().insert(packageName, activityName, code, 0, 0, system, false)
            .map(result -> {
              Timber.d("Create result: %d", result);
              return true;
            });
      } else {
        Timber.d("Modify the database to delete entry for: %s %s", packageName, activityName);

        return getPadLockDB().deleteWithPackageActivityName(packageName, activityName)
            .map(result -> {
              Timber.d("Delete result: %d", result);
              return false;
            });
      }
    }).toList().map(result -> {
      Timber.d(
          "To prevent a bunch of events from occurring for each list entry, we flatten to just a single result");

      // We return the original request
      return allCreate;
    });
  }

  @Override public void setShownOnBoarding() {
    preferences.setLockInfoDialogOnBoard();
  }

  @NonNull @Override public Observable<Boolean> hasShownOnBoarding() {
    return Observable.defer(() -> Observable.just(preferences.isLockInfoDialogOnBoard()));
  }
}
