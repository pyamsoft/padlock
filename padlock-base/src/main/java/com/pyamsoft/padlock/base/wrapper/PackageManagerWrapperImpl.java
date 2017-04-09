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

package com.pyamsoft.padlock.base.wrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

class PackageManagerWrapperImpl implements PackageManagerWrapper {

  @SuppressWarnings("WeakerAccess") @NonNull static final String ANDROID_SYSTEM_UI_PACKAGE =
      "com.android.systemui";
  @SuppressWarnings("WeakerAccess") @NonNull static final String ANDROID_PACKAGE = "android";
  @SuppressWarnings("WeakerAccess") @NonNull final PackageManager packageManager;

  @Inject PackageManagerWrapperImpl(@NonNull Context context) {
    packageManager = context.getApplicationContext().getPackageManager();
  }

  @NonNull @Override
  public Observable<Drawable> loadDrawableForPackageOrDefault(@NonNull String packageName) {
    return Observable.fromCallable(() -> {
      Drawable image;
      try {
        image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager);
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "PackageManager error");
        image = packageManager.getDefaultActivityIcon();
      }

      return image;
    });
  }

  @NonNull @Override
  public Single<List<String>> getActivityListForPackage(@NonNull String packageName) {
    return Single.fromCallable(() -> {
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
      } catch (Exception e) {
        Timber.e(e, "PackageManager error, return what we have for %s", packageName);
      }
      return activityEntries;
    });
  }

  @SuppressWarnings("WeakerAccess") @NonNull @CheckResult
  Flowable<ApplicationInfo> getInstalledApplications() {
    return Flowable.fromCallable(() -> {
      final Process process;
      boolean caughtPermissionDenial = false;
      final List<String> packageNames = new ArrayList<>();
      try {
        // The adb shell command pm list packages returns a list of packages in the following format:
        //
        // package:<package name>
        //
        // but it is not a victim of BinderTransaction failures so it will be able to better handle
        // large sets of applications.
        final String command;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          // Android N moves package list command to a different, same format, faster command
          command = "cmd package list packages";
        } else {
          command = "pm list packages";
        }
        process = Runtime.getRuntime().exec(command);
        try (final BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
          String line = bufferedReader.readLine();
          while (line != null && !line.isEmpty()) {
            if (line.startsWith("Permission Denial")) {
              Timber.e("Command resulted in permission denial");
              caughtPermissionDenial = true;
              break;
            }
            packageNames.add(line);
            line = bufferedReader.readLine();
          }
        }

        if (caughtPermissionDenial) {
          throw new IllegalStateException("Error running command: " + command);
        }

        // Will always be 0
      } catch (IOException e) {
        Timber.e(e, "Error running shell command");
        packageNames.clear();
      }

      return packageNames;
    })
        .flatMap(Flowable::fromIterable)
        .onBackpressureBuffer(16, () -> Timber.e(
            "PackageManagerWrapperImpl getInstalledApplications backpressure overflow"))
        .map(packageNameWithPrefix -> packageNameWithPrefix.replaceFirst("^package:", ""))
        .flatMap(
            packageName -> getApplicationInfo(packageName).toFlowable(BackpressureStrategy.BUFFER));
  }

  @NonNull @Override public Single<List<ApplicationInfo>> getActiveApplications() {
    return getInstalledApplications().flatMap(info -> {
      if (!info.enabled) {
        Timber.i("Application %s is disabled", info.packageName);
        return Flowable.empty();
      }

      if (ANDROID_PACKAGE.equals(info.packageName)) {
        Timber.i("Application %s is Android", info.packageName);
        return Flowable.empty();
      }

      if (ANDROID_SYSTEM_UI_PACKAGE.equals(info.packageName)) {
        Timber.i("Application %s is System UI", info.packageName);
        return Flowable.empty();
      }

      Timber.d("Successfully processed application: %s", info.packageName);
      return Flowable.just(info);
    }).toList();
  }

  @NonNull @Override
  public Observable<ApplicationInfo> getApplicationInfo(@NonNull String packageName) {
    return Observable.defer(() -> {
      try {
        return Observable.just(packageManager.getApplicationInfo(packageName, 0));
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "onError getApplicationInfo: '" + packageName + "'");
        return Observable.empty();
      }
    });
  }

  @NonNull @Override public Observable<String> loadPackageLabel(@NonNull ApplicationInfo info) {
    return Observable.fromCallable(() -> info.loadLabel(packageManager).toString());
  }

  @NonNull @Override public Observable<String> loadPackageLabel(@NonNull String packageName) {
    return Observable.defer(() -> {
      try {
        return Observable.just(packageManager.getApplicationInfo(packageName, 0));
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "onError loadPackageLabel: '" + packageName + "'");
        return Observable.empty();
      }
    }).flatMap(this::loadPackageLabel);
  }

  @NonNull @Override public Observable<ActivityInfo> getActivityInfo(@NonNull String packageName,
      @NonNull String activityName) {
    return Observable.defer(() -> {
      if (packageName.isEmpty() || activityName.isEmpty()) {
        return Observable.empty();
      }

      final ComponentName componentName = new ComponentName(packageName, activityName);
      try {
        return Observable.just(packageManager.getActivityInfo(componentName, 0));
      } catch (PackageManager.NameNotFoundException e) {
        Timber.e(e, "onError getActivityInfo: '" + packageName + "', '" + activityName + "'");
        return Observable.empty();
      }
    });
  }
}
