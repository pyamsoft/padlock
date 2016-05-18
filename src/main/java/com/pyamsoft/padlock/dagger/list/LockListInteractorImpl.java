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

  @Override public boolean hasShownOnBoarding() {
    return preferences.isOnBoard();
  }

  @Override public boolean isSystemVisible() {
    return preferences.isSystemVisible();
  }

  @Override public void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }

  @Override public void setShownOnBoarding() {
    preferences.setOnBoard();
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
    return PadLockEntry.queryAll(appContext).first();
  }

  @WorkerThread @Override public boolean isSystemApplication(@NonNull ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }
}
