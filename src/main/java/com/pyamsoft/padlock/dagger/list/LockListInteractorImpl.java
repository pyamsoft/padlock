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
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

final class LockListInteractorImpl implements LockListInteractor {

  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferences preferences;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;

  @Inject public LockListInteractorImpl(@NonNull PackageManagerWrapper packageManagerWrapper,
      final @NonNull Context context, final @NonNull PadLockPreferences preferences) {
    this.packageManagerWrapper = packageManagerWrapper;
    appContext = context.getApplicationContext();
    this.preferences = preferences;
  }

  @Override public Observable<Boolean> hasShownOnBoarding() {
    return Observable.defer(() -> Observable.just(preferences.isOnBoard()));
  }

  @Override public Observable<Boolean> isSystemVisible() {
    return Observable.defer(() -> Observable.just(preferences.isSystemVisible()));
  }

  @Override public void setSystemVisible(boolean visible) {
    preferences.setSystemVisible(visible);
  }

  @Override public void setShownOnBoarding() {
    preferences.setOnBoard();
  }

  @Override @NonNull public Observable<List<ApplicationInfo>> getApplicationInfoList() {
    return packageManagerWrapper.getActiveApplications()
        .filter(applicationInfo -> !isSystemApplication(applicationInfo).toBlocking().first()
            || isSystemVisible().toBlocking().first())
        .toList();
  }

  @NonNull @Override public Observable<List<PadLockEntry>> getAppEntryList() {
    return PadLockDB.with(appContext).queryAll().first();
  }

  @Override public Observable<Boolean> isSystemApplication(@NonNull ApplicationInfo info) {
    return Observable.defer(() -> Observable.just((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
  }

  @NonNull @Override public Observable<String> loadPackageLabel(@NonNull ApplicationInfo info) {
    return packageManagerWrapper.loadPackageLabel(info);
  }
}
