/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.base.wrapper

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import javax.inject.Inject

internal class PackageManagerWrapperImpl @Inject internal constructor(
    context: Context) : PackageActivityManager, PackageApplicationManager, PackageLabelManager, PackageDrawableManager {

  private val packageManager: PackageManager = context.applicationContext.packageManager

  override fun loadDrawableForPackageOrDefault(packageName: String): Single<Drawable> {
    return Single.fromCallable {
      val image: Drawable
      image = try {
        // Assign
        packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager)
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "PackageManager error")
        // Assign
        packageManager.defaultActivityIcon
      }
      return@fromCallable image
    }
  }

  override fun getActivityListForPackage(packageName: String): Single<List<String>> {
    return Single.fromCallable<List<String>> {
      val activityEntries: MutableList<String> = ArrayList()
      try {
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        packageInfo.activities?.mapTo(activityEntries) { it.name }
      } catch (e: Exception) {
        Timber.e(e, "PackageManager error, return what we have for %s", packageName)
      }
      return@fromCallable activityEntries
    }
  }

  @CheckResult
  private fun getInstalledApplications(): Observable<ApplicationInfo> {
    return Single.fromCallable {
      val process: Process
      var caughtPermissionDenial = false
      val packageNames: MutableList<String> = ArrayList()
      try {
        // The adb shell command pm list packages returns a list of packages in the following format:
        //
        // package:<package name>
        //
        // but it is not a victim of BinderTransaction failures so it will be able to better handle
        // large sets of applications.
        val command: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          // Android N moves package list command to a different, same format, faster command

          // Assign
          "cmd package list packages"
        } else {
          // Assign
          "pm list packages"
        }
        process = Runtime.getRuntime().exec(command)
        BufferedReader(
            InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use {
          var line: String? = it.readLine()
          while (line != null && line.isNotBlank()) {
            if (line.startsWith("Permission Denial")) {
              Timber.e("Command resulted in permission denial")
              caughtPermissionDenial = true
              break
            }
            packageNames.add(line)
            line = it.readLine()
          }
        }

        if (caughtPermissionDenial) {
          throw IllegalStateException("Error running command: $command, throw and bail")
        }

        // Will always be 0
      } catch (e: IOException) {
        Timber.e(e, "Error running shell command, return what we have")
      }

      return@fromCallable packageNames
    }.flatMapObservable { Observable.fromIterable(it) }.map {
      it.replaceFirst("^package:".toRegex(), "")
    }.flatMapSingle { getApplicationInfo(it) }
  }

  override fun getActiveApplications(): Single<List<ApplicationInfo>> {
    return getInstalledApplications().flatMap<ApplicationInfo> {
      if (!it.enabled) {
        Timber.i("Application %s is disabled", it.packageName)
        return@flatMap Observable.empty()
      }

      if (ANDROID_PACKAGE == it.packageName) {
        Timber.i("Application %s is Android", it.packageName)
        return@flatMap Observable.empty()
      }

      if (ANDROID_SYSTEM_UI_PACKAGE == it.packageName) {
        Timber.i("Application %s is System UI", it.packageName)
        return@flatMap Observable.empty()
      }

      Timber.d("Successfully processed application: %s", it.packageName)
      return@flatMap Observable.just(it)
    }.toList()
  }

  override fun getApplicationInfo(packageName: String): Single<ApplicationInfo> {
    return Single.defer {
      try {
        return@defer Single.just(packageManager.getApplicationInfo(packageName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError getApplicationInfo: '$packageName'")
        throw Exceptions.propagate(e)
      }
    }
  }

  override fun loadPackageLabel(info: ApplicationInfo): Single<String> =
      Single.fromCallable { info.loadLabel(packageManager) }.map { it.toString() }

  override fun loadPackageLabel(packageName: String): Single<String> {
    return Single.defer {
      try {
        return@defer Single.just(packageManager.getApplicationInfo(packageName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError loadPackageLabel: '$packageName'")
        throw Exceptions.propagate(e)
      }
    }.flatMap { loadPackageLabel(it) }
  }

  override fun getActivityInfo(packageName: String, activityName: String): Single<ActivityInfo> {
    return Single.defer {
      if (packageName.isEmpty() || activityName.isEmpty()) {
        return@defer Single.just(ActivityInfo())
      }
      val componentName = ComponentName(packageName, activityName)
      try {
        return@defer Single.just(packageManager.getActivityInfo(componentName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        // We intentionally leave out the throwable in the call to Timber or logs get too noisy
        Timber.e("Could not get ActivityInfo for: '$packageName', '$activityName'")
        throw Exceptions.propagate(e)
      }
    }
  }

  companion object {
    const val ANDROID_SYSTEM_UI_PACKAGE: String = "com.android.systemui"
    const val ANDROID_PACKAGE: String = "android"
  }

}