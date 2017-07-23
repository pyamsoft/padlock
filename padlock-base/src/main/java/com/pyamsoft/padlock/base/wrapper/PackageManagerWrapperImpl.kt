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
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import javax.inject.Inject

internal class PackageManagerWrapperImpl @Inject internal constructor(
    context: Context) : PackageManagerWrapper {

  protected @JvmField val packageManager: PackageManager = context.applicationContext.packageManager

  override fun loadDrawableForPackageOrDefault(packageName: String): Single<Drawable> {
    return Single.fromCallable {
      var image: Drawable
      try {
        image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager)
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "PackageManager error")
        image = packageManager.defaultActivityIcon
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
    return Single.fromCallable<List<String>> {
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
        val command: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          // Android N moves package list command to a different, same format, faster command
          command = "cmd package list packages"
        } else {
          command = "pm list packages"
        }
        process = Runtime.getRuntime().exec(command)
        BufferedReader(
            InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use {
          var line: String? = it.readLine()
          while (line != null && !line.isEmpty()) {
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
    }.flatMapMaybe { getApplicationInfo(it) }
  }

  override fun getActiveApplications(): Single<List<ApplicationInfo>> {
    return getInstalledApplications().flatMap<ApplicationInfo> { info ->
      if (!info.enabled) {
        Timber.i("Application %s is disabled", info.packageName)
        return@flatMap Observable.empty()
      }

      if (ANDROID_PACKAGE == info.packageName) {
        Timber.i("Application %s is Android", info.packageName)
        return@flatMap Observable.empty()
      }

      if (ANDROID_SYSTEM_UI_PACKAGE == info.packageName) {
        Timber.i("Application %s is System UI", info.packageName)
        return@flatMap Observable.empty()
      }

      Timber.d("Successfully processed application: %s", info.packageName)
      return@flatMap Observable.just(info)
    }.toList()
  }

  override fun getApplicationInfo(packageName: String): Maybe<ApplicationInfo> {
    return Maybe.defer<ApplicationInfo> {
      try {
        return@defer Maybe.just(packageManager.getApplicationInfo(packageName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError getApplicationInfo: '$packageName'")
        return@defer Maybe.empty()
      }
    }
  }

  override fun loadPackageLabel(info: ApplicationInfo): Maybe<String> {
    return Maybe.fromCallable { info.loadLabel(packageManager) }.map { it.toString() }
  }

  override fun loadPackageLabel(packageName: String): Maybe<String> {
    return Maybe.defer<ApplicationInfo> {
      try {
        return@defer Maybe.just(packageManager.getApplicationInfo(packageName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError loadPackageLabel: '$packageName'")
        return@defer Maybe.empty()
      }
    }.flatMap { loadPackageLabel(it) }
  }

  override fun getActivityInfo(packageName: String,
      activityName: String): Maybe<ActivityInfo> {
    return Maybe.defer<ActivityInfo> {
      if (packageName.isEmpty() || activityName.isEmpty()) {
        return@defer Maybe.empty()
      }
      val componentName = ComponentName(packageName, activityName)
      try {
        return@defer Maybe.just(packageManager.getActivityInfo(componentName, 0))
      } catch (e: PackageManager.NameNotFoundException) {
        // We intentionally leave out the throwable in the call to Timber or logs get too noisy
        Timber.e("Could not get ActivityInfo for: '$packageName', '$activityName'")
        return@defer Maybe.empty()
      }
    }
  }

  companion object {
    const val ANDROID_SYSTEM_UI_PACKAGE: String = "com.android.systemui"
    const val ANDROID_PACKAGE: String = "android"
  }

}