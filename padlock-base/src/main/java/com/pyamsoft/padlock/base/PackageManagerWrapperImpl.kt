/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.base

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.api.*
import com.pyamsoft.padlock.model.ApplicationItem
import com.pyamsoft.padlock.model.Excludes
import com.pyamsoft.padlock.model.IconHolder
import com.pyamsoft.pydroid.optional.Optional.Present
import com.pyamsoft.pydroid.optional.asOptional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PackageManagerWrapperImpl @Inject internal constructor(
    context: Context,
    private val listPreferences: LockListPreferences
) : PackageActivityManager,
    PackageApplicationManager,
    PackageLabelManager,
    PackageIconManager<Drawable> {

  private val packageManager: PackageManager = context.applicationContext.packageManager

  override fun loadIconForPackageOrDefault(packageName: String): Single<IconHolder<Drawable>> {
    return Single.fromCallable {
      val image: Drawable
      image = try {
        // Assign
        packageManager.getApplicationInfo(packageName, 0)
            .loadIcon(packageManager)
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "PackageManager error")
        // Assign
        packageManager.defaultActivityIcon
      }
      return@fromCallable IconHolder(image)
    }
  }

  override fun getActivityListForPackage(packageName: String): Single<List<String>> {
    return Single.fromCallable {
      val activityEntries: MutableList<String> = ArrayList()
      try {
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES
        )
        packageInfo.activities?.mapTo(activityEntries) { it.name }
      } catch (e: Exception) {
        Timber.e(e, "PackageManager error, return what we have for %s", packageName)
      }
      return@fromCallable activityEntries
    }
        .flatMapObservable { Observable.fromIterable(it) }
        .filter { !Excludes.isLockScreen(packageName, it) }
        .filter { !Excludes.isPackageExcluded(packageName) }
        .filter { !Excludes.isClassExcluded(it) }
        .toSortedList()
  }

  @CheckResult
  private fun getInstalledApplications(): Observable<ApplicationItem> {
    return Single.fromCallable { packageManager.getInstalledApplications(0) }
        .flatMapObservable { Observable.fromIterable(it) }
        .flatMapSingle { getApplicationInfo(it) }
        .filter { !it.isEmpty() }
  }

  override fun getActiveApplications(): Single<List<ApplicationItem>> {
    return getInstalledApplications().flatMap {
      return@flatMap when {
        !it.enabled -> {
          Timber.i("Application %s is disabled", it.packageName)
          Observable.empty()
        }
        it.system && !listPreferences.isSystemVisible() -> {
          Timber.i("Hide system application: %s", it.packageName)
          Observable.empty()
        }
        Excludes.isPackageExcluded(it.packageName) -> {
          Timber.i("Application %s is excluded", it.packageName)
          Observable.empty()
        }
        else -> {
          Timber.d("Successfully processed application: %s", it.packageName)
          Observable.just(it)
        }
      }
    }
        .toList()
  }

  @CheckResult
  private fun getApplicationInfo(info: ApplicationInfo?): Single<ApplicationItem> {
    return Single.fromCallable {
      when (info) {
        null -> ApplicationItem.EMPTY
        else -> ApplicationItem.create(info.packageName, info.system(), info.enabled)
      }
    }
  }

  override fun getApplicationInfo(packageName: String): Single<ApplicationItem> {
    return Single.defer {
      var info: ApplicationInfo?
      try {
        info = packageManager.getApplicationInfo(packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError getApplicationInfo: '$packageName'")
        info = null
      }
      return@defer getApplicationInfo(info)
    }
  }

  @CheckResult
  private fun ApplicationInfo.system(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0

  override fun loadPackageLabel(packageName: String): Single<String> {
    return Single.defer {
      try {
        return@defer Single.just(
            packageManager.getApplicationInfo(packageName, 0).asOptional()
        )
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "onError loadPackageLabel: '$packageName'")
        throw Exceptions.propagate(e)
      }
    }
        .flatMap {
          return@flatMap when (it) {
            is Present -> loadPackageLabel(it.value)
            else -> Single.just("")
          }
        }
  }

  @CheckResult
  private fun loadPackageLabel(info: ApplicationInfo): Single<String> = Single.fromCallable {
    info.loadLabel(packageManager)?.toString() ?: ""
  }

  override fun isValidActivity(
      packageName: String,
      activityName: String
  ): Single<Boolean> {
    return Single.defer {
      if (packageName.isEmpty() || activityName.isEmpty()) {
        return@defer Single.just(false)
      }

      val componentName = ComponentName(packageName, activityName)
      try {
        val info: ActivityInfo? = packageManager.getActivityInfo(componentName, 0)
        return@defer Single.just(info != null)
      } catch (e: PackageManager.NameNotFoundException) {
        // We intentionally leave out the throwable in the call to Timber or logs get too noisy
        Timber.e("Could not get ActivityInfo for: '$packageName', '$activityName'")
        return@defer Single.just(false)
      }
    }
  }
}
