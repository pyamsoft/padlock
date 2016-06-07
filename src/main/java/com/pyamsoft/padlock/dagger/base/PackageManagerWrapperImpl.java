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

package com.pyamsoft.padlock.dagger.base;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.service.LockServiceInteractor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public final class PackageManagerWrapperImpl implements PackageManagerWrapper {

  @NonNull private final PackageManager packageManager;

  @Inject public PackageManagerWrapperImpl(@NonNull Context context) {
    this.packageManager = context.getApplicationContext().getPackageManager();
  }

  @NonNull @Override public Drawable loadDrawableForPackageOrDefault(@NonNull String packageName) {
    Drawable image;
    try {
      image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager);
    } catch (PackageManager.NameNotFoundException e) {
      Timber.e(e, "PackageManager error");
      image = packageManager.getDefaultActivityIcon();
    }
    return image;
  }

  @NonNull @Override public List<String> getActivityListForPackage(@NonNull String packageName) {
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
    return activityEntries;
  }

  @NonNull @Override public List<ApplicationInfo> getActiveApplications() {
    final List<Integer> removeIndexes = new ArrayList<>();

    final List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(0);
    final int size = applicationInfos.size();
    for (int i = 0; i < size; ++i) {
      final ApplicationInfo info = applicationInfos.get(i);
      Timber.d("Application: %s", info.packageName);
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

    return applicationInfos;
  }

  @NonNull @Override public String loadPackageLabel(@NonNull ApplicationInfo info) {
    return info.loadLabel(packageManager).toString();
  }
}
