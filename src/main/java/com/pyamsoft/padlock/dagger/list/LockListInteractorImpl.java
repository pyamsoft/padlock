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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.dagger.service.LockServiceInteractor;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

final class LockListInteractorImpl implements LockListInteractor {

  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;

  @Inject public LockListInteractorImpl(final @NonNull Context context,
      final @NonNull PadLockPreferences preferences) {
    appContext = context.getApplicationContext();
    this.preferences = preferences;
  }

  @WorkerThread @NonNull @Override public PackageManager getPackageManager() {
    return appContext.getPackageManager();
  }

  @WorkerThread @Override @NonNull public Observable<List<PackageInfo>> getPackageInfoList() {
    return Observable.defer(() -> Observable.from(getPackageManager().getInstalledPackages(0)))
        .filter(packageInfo -> packageInfo != null)
        .filter(packageInfo -> {
          final ApplicationInfo appInfo = packageInfo.applicationInfo;
          return appInfo != null && !(!appInfo.enabled || (isSystemApplication(appInfo)
              && !preferences.isSystemVisible()) || appInfo.packageName.equals(
              LockServiceInteractor.ANDROID_PACKAGE) || appInfo.packageName.equals(
              LockServiceInteractor.ANDROID_SYSTEM_UI_PACKAGE));
        })
        .toList();
  }

  @WorkerThread @NonNull @Override public Observable<List<PadLockEntry>> getAppEntryList() {
    return PadLockDB.with(appContext)
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
        .mapToList(PadLockEntry.MAPPER::map)
        .filter(padLockEntries -> padLockEntries != null)
        .first();
  }

  @WorkerThread @Override public boolean isSystemApplication(ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @WorkerThread @NonNull @Override public Observable<Boolean> isSystemVisible() {
    return Observable.defer(() -> Observable.just(preferences.isSystemVisible()))
        .map(aBoolean -> aBoolean == null ? false : aBoolean);
  }

  @WorkerThread @Override @NonNull public Observable<Boolean> hasShownOnBoarding() {
    return Observable.defer(() -> Observable.just(preferences.isOnBoard()))
        .map(aBoolean -> aBoolean == null ? false : aBoolean);
  }

  @NonNull @Override public Observable<Boolean> setShownOnBoarding() {
    return Observable.defer(() -> {
      preferences.setOnBoard();
      return Observable.just(true);
    });
  }

  @NonNull @Override public Observable<Boolean> setSystemVisible(boolean visible) {
    return Observable.defer(() -> {
      preferences.setSystemVisible(visible);
      return Observable.just(visible);
    });
  }
}
