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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockListInteractorImpl extends LockCommonInteractorImpl implements LockListInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @NonNull final PackageManagerWrapper packageManagerWrapper;

  @Inject LockListInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper,
      final @NonNull Context context, final @NonNull PadLockPreferences preferences) {
    super(context);
    this.packageManagerWrapper = packageManagerWrapper;
    this.preferences = preferences;
  }

  @NonNull @Override public Observable<Boolean> hasShownOnBoarding() {
    return Observable.defer(() -> Observable.just(preferences.isOnBoard()));
  }

  @NonNull @Override public Observable<Boolean> isSystemVisible() {
    return Observable.defer(() -> Observable.just(preferences.isSystemVisible()));
  }

  @Override public void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }

  @Override public void setShownOnBoarding() {
    preferences.setOnBoard();
  }

  @Override @NonNull public Observable<List<ApplicationInfo>> getApplicationInfoList() {
    return packageManagerWrapper.getActiveApplications().filter(applicationInfo -> {
      // KLUDGE Blocking
      final boolean systemVisible = isSystemVisible().toBlocking().first();
      if (systemVisible) {
        // If system visible, we show all apps
        return true;
      } else {
        if (isSystemApplication(applicationInfo)) {
          // Application is system but system apps are hidden
          Timber.w("Hide system application: %s", applicationInfo.packageName);
          return false;
        } else {
          return true;
        }
      }
    }).filter(applicationInfo -> {
      // KLUDGE blocking
      if (packageManagerWrapper.getActivityListForPackage(applicationInfo.packageName)
          .toList()
          .toBlocking()
          .first()
          .size() == 0) {
        Timber.w("Exclude package %s because it has no activities", applicationInfo.packageName);
        return false;
      } else {
        return true;
      }
    }).toList();
  }

  @NonNull @Override public Observable<List<PadLockEntry>> getAppEntryList() {
    return PadLockDB.with(getAppContext()).queryAll().first();
  }

  @CheckResult boolean isSystemApplication(@NonNull ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @WorkerThread @NonNull @Override
  public AppEntry createFromPackageInfo(@NonNull ApplicationInfo info, boolean locked) {
    Timber.d("Create AppEntry from package info: %s", info.packageName);
    return AppEntry.builder()
        .name(packageManagerWrapper.loadPackageLabel(info).toBlocking().first())
        .packageName(info.packageName)
        .system(isSystemApplication(info))
        .locked(locked)
        .build();
  }
}
