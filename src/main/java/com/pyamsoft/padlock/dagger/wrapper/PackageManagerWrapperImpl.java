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

package com.pyamsoft.padlock.dagger.wrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.service.LockServiceInteractor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

class PackageManagerWrapperImpl implements PackageManagerWrapper {

  @SuppressWarnings("WeakerAccess") @NonNull final PackageManager packageManager;

  @Inject PackageManagerWrapperImpl(@NonNull Context context) {
    this.packageManager = context.getApplicationContext().getPackageManager();
  }

  @NonNull @Override
  public Observable<Drawable> loadDrawableForPackageOrDefault(@NonNull String packageName) {
    return Observable.defer(() -> {
      Drawable image;
      try {
        image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager);
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "PackageManager error");
        image = packageManager.getDefaultActivityIcon();
      }
      return Observable.just(image);
    });
  }

  @NonNull @Override
  public Observable<String> getActivityListForPackage(@NonNull String packageName) {
    return Observable.defer(() -> {
      final List<String> activityEntries = new ArrayList<>();
      try {
        final PackageInfo packageInfo =
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        final ActivityInfo[] activities = packageInfo.activities;
        if (activities != null) {
          for (final ActivityInfo activityInfo : activities) {
            activityEntries.add(activityInfo.name);
          }
        }
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "PackageManager error");
        activityEntries.clear();
      }
      return Observable.from(activityEntries);
    });
  }

  @NonNull @Override public Observable<ApplicationInfo> getActiveApplications() {
    return Observable.defer(() -> {
      final List<Integer> removeIndexes = new ArrayList<>();

      final List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(0);
      if (applicationInfos == null) {
        Timber.e("Application list is empty");
        return Observable.empty();
      }

      final int size = applicationInfos.size();
      for (int i = 0; i < size; ++i) {
        final ApplicationInfo info = applicationInfos.get(i);
        if (!info.enabled) {
          Timber.d("Application %s at %d is disabled", info.packageName, i);
          removeIndexes.add(i);
          continue;
        }

        if (info.packageName.equals(LockServiceInteractor.ANDROID_PACKAGE)) {
          Timber.d("Application %s at %d is Android", info.packageName, i);
          removeIndexes.add(i);
          continue;
        }

        if (info.packageName.equals(LockServiceInteractor.ANDROID_SYSTEM_UI_PACKAGE)) {
          Timber.d("Application %s at %d is System UI", info.packageName, i);
          removeIndexes.add(i);
        }
      }

      int removedIndexOffset = 0;
      for (final int index : removeIndexes) {
        Timber.d("Remove index at %d", index);
        applicationInfos.remove(index - removedIndexOffset);
        ++removedIndexOffset;
      }

      Timber.d("Application size: %d", applicationInfos.size());
      return Observable.from(applicationInfos);
    });
  }

  @NonNull @Override
  public Observable<ApplicationInfo> getApplicationInfo(@NonNull String packageName) {
    return Observable.defer(() -> {
      try {
        return Observable.just(packageManager.getApplicationInfo(packageName, 0));
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "onError getApplicationInfo");
        return Observable.empty();
      }
    });
  }

  @NonNull @Override public Observable<String> loadPackageLabel(@NonNull ApplicationInfo info) {
    return Observable.defer(() -> Observable.just(info.loadLabel(packageManager).toString()));
  }

  @NonNull @Override public Observable<String> loadPackageLabel(@NonNull String packageName) {
    return Observable.defer(() -> {
      try {
        final ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        return loadPackageLabel(applicationInfo);
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "EXCEPTION");
        return Observable.just("");
      }
    });
  }

  @NonNull @Override public Observable<ActivityInfo> getActivityInfo(@NonNull String packageName,
      @NonNull String activityName) {
    return Observable.defer(() -> {
      if (packageName.isEmpty() || activityName.isEmpty()) {
        return Observable.empty();
      }

      final ComponentName componentName = new ComponentName(packageName, activityName);
      try {
        final ActivityInfo activityInfo = packageManager.getActivityInfo(componentName, 0);
        return Observable.just(activityInfo);
      } catch (PackageManager.NameNotFoundException e) {
        return Observable.empty();
      }
    });
  }
}
