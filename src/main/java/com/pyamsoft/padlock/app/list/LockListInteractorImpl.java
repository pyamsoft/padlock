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

package com.pyamsoft.padlock.app.list;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.lock.LockCommonInteractorImpl;
import com.pyamsoft.padlock.app.service.LockServiceInteractor;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

public final class LockListInteractorImpl extends LockCommonInteractorImpl
    implements LockListInteractor {

  @NonNull private final PadLockPreferences preferences;

  @Inject public LockListInteractorImpl(final @NonNull Context context,
      final @NonNull PadLockPreferences preferences) {
    super(context);
    this.preferences = preferences;
  }

  @NonNull @Override public PackageManager getPackageManager() {
    return getAppContext().getPackageManager();
  }

  @Override @NonNull public Observable<List<PackageInfo>> getPackageInfoList() {
    return Observable.defer(() -> Observable.from(getPackageManager().getInstalledPackages(0)))
        .filter(packageInfo -> {
          Timber.d("Filter out packages with null application info");
          final ApplicationInfo appInfo = packageInfo.applicationInfo;
          return appInfo != null && !(!appInfo.enabled || (isSystemApplication(appInfo)
              && !isSystemVisible()) || appInfo.packageName.equals(
              LockServiceInteractor.ANDROID_PACKAGE) || appInfo.packageName.equals(
              LockServiceInteractor.ANDROID_SYSTEM_UI_PACKAGE));
        })
        .filter(packageInfo -> packageInfo != null)
        .toList();
  }

  @NonNull @Override public Observable<List<PadLockEntry>> getAppEntryList() {
    Timber.d("getPackageInfoList");
    return PadLockDB.with(getAppContext())
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.ALL_ENTRIES)
        .mapToList(PadLockEntry.MAPPER::map);
  }

  @Override public boolean isSystemApplication(ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @Override public boolean isSystemVisible() {
    return preferences.isSystemVisible();
  }

  @Override public void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }

  @Override public boolean hasShownOnBoarding() {
    return preferences.isOnBoard();
  }

  @Override public void setShownOnBoarding() {
    preferences.setOnBoard();
  }
}
