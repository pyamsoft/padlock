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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

public class PackageManagerWrapperImpl implements PackageManagerWrapper {

  @NonNull private final Context appContext;

  @Inject public PackageManagerWrapperImpl(@NonNull Context context) {
    this.appContext = context.getApplicationContext();
  }

  @NonNull @Override public Drawable loadDrawableForPackageOrDefault(@NonNull String packageName) {
    final PackageManager packageManager = appContext.getPackageManager();
    Drawable image;
    try {
      image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager);
    } catch (PackageManager.NameNotFoundException e) {
      image = packageManager.getDefaultActivityIcon();
    }
    return image;
  }

  @NonNull @Override public List<String> getActivityListForPackage(@NonNull String packageName) {
    final PackageManager packageManager = appContext.getPackageManager();
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
}
