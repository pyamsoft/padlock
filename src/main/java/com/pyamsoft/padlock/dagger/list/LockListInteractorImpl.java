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

import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.PadLockDB;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class LockListInteractorImpl extends LockCommonInteractorImpl implements LockListInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final PadLockPreferences preferences;
  @SuppressWarnings("WeakerAccess") @NonNull final PackageManagerWrapper packageManagerWrapper;

  @Inject LockListInteractorImpl(PadLockDB padLockDB,
      @NonNull PackageManagerWrapper packageManagerWrapper,
      @NonNull PadLockPreferences preferences) {
    super(padLockDB);
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

  @NonNull @Override public Observable<ApplicationInfo> getActiveApplications() {
    return packageManagerWrapper.getActiveApplications();
  }

  @NonNull @Override
  public Observable<String> getActivityListForApplication(@NonNull ApplicationInfo info) {
    return packageManagerWrapper.getActivityListForPackage(info.packageName);
  }

  @NonNull @Override public Observable<List<PadLockEntry.AllEntries>> getAppEntryList() {
    return getPadLockDB().queryAll().first();
  }

  @Override @CheckResult public boolean isSystemApplication(@NonNull ApplicationInfo info) {
    return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
  }

  @NonNull @Override
  public Observable<AppEntry> createFromPackageInfo(@NonNull String packageName, boolean locked) {
    Timber.d("Create AppEntry from package info: %s", packageName);
    return packageManagerWrapper.getApplicationInfo(packageName)
        .map(info -> AppEntry.builder()
            .name(packageManagerWrapper.loadPackageLabel(info).toBlocking().first())
            .packageName(packageName)
            .system(isSystemApplication(info))
            .locked(locked)
            .build());
  }
}
