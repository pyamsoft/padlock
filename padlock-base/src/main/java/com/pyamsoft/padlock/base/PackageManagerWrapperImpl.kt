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
import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.padlock.api.packagemanager.PackageApplicationManager
import com.pyamsoft.padlock.api.packagemanager.PackageLabelManager
import com.pyamsoft.padlock.api.preferences.LockListPreferences
import com.pyamsoft.padlock.model.ApplicationItem
import com.pyamsoft.padlock.model.Excludes
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

internal class PackageManagerWrapperImpl @Inject internal constructor(
  context: Context,
  private val enforcer: Enforcer,
  private val listPreferences: LockListPreferences
) : PackageActivityManager, PackageApplicationManager, PackageLabelManager {

  private val packageManager = context.applicationContext.packageManager
  private val defaultIcon: Drawable by lazy { packageManager.defaultActivityIcon }

  override fun getDefaultActivityIcon(): Drawable {
    return defaultIcon
  }

  override fun getActivityListForPackage(packageName: String): Single<List<String>> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
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
        .flatMapObservable {
          enforcer.assertNotOnMainThread()
          return@flatMapObservable Observable.fromIterable(it)
        }
        .filter { !Excludes.isLockScreen(packageName, it) }
        .filter { !Excludes.isPackageExcluded(packageName) }
        .filter { !Excludes.isClassExcluded(it) }
        .toSortedList()
  }

  @CheckResult
  private fun getInstalledApplications(): Observable<ApplicationItem> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable packageManager.getInstalledApplications(0)
    }
        .flatMapObservable {
          enforcer.assertNotOnMainThread()
          return@flatMapObservable Observable.fromIterable(it)
        }
        .flatMapSingle {
          enforcer.assertNotOnMainThread()
          return@flatMapSingle getApplicationInfo(it)
        }
        .filter { !it.isEmpty() }
  }

  override fun getActiveApplications(): Single<List<ApplicationItem>> {
    return getInstalledApplications().flatMap {
      enforcer.assertNotOnMainThread()
      return@flatMap when {
        !it.enabled -> Observable.empty()
        it.system && !listPreferences.isSystemVisible() -> Observable.empty()
        Excludes.isPackageExcluded(it.packageName) -> Observable.empty()
        else -> Observable.just(it)
      }
    }
        .toList()
  }

  @CheckResult
  private fun getApplicationInfo(info: ApplicationInfo?): Single<ApplicationItem> {
    return Single.fromCallable {
      enforcer.assertNotOnMainThread()
      return@fromCallable when (info) {
        null -> ApplicationItem.EMPTY
        else -> ApplicationItem.create(info.packageName, info.icon, info.system(), info.enabled)
      }
    }
  }

  override fun getApplicationInfo(packageName: String): Single<ApplicationItem> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      var info: ApplicationInfo?
      try {
        info = packageManager.getApplicationInfo(packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "Error getApplicationInfo: '$packageName'")
        info = null
      }
      return@defer getApplicationInfo(info)
    }
  }

  @CheckResult
  private fun ApplicationInfo.system(): Boolean = flags and ApplicationInfo.FLAG_SYSTEM != 0

  override fun loadPackageLabel(packageName: String): Single<String> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      try {
        return@defer Single.just(
            packageManager.getApplicationInfo(packageName, 0).asOptional()
        )
      } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "Error loadPackageLabel: '$packageName'")
        throw Exceptions.propagate(e)
      }
    }
        .flatMap {
          enforcer.assertNotOnMainThread()
          return@flatMap when (it) {
            is Present -> loadPackageLabel(it.value)
            else -> Single.just("")
          }
        }
  }

  @CheckResult
  private fun loadPackageLabel(info: ApplicationInfo): Single<String> = Single.fromCallable {
    enforcer.assertNotOnMainThread()
    return@fromCallable info.loadLabel(packageManager)
        .toString()
  }

  override fun isValidActivity(
    packageName: String,
    activityName: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
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
