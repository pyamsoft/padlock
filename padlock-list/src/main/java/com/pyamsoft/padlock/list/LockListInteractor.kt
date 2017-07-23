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

package com.pyamsoft.padlock.list

import android.content.pm.ApplicationInfo
import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDB
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.base.db.PadLockEntry.AllEntries
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.preference.OnboardingPreferences
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper
import com.pyamsoft.padlock.model.AppEntry
import com.pyamsoft.pydroid.helper.Optional
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.Collections
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Singleton


@Singleton internal class LockListInteractor @Inject internal constructor(
    protected @JvmField val padLockDB: PadLockDB,
    protected @JvmField val packageManager: PackageManagerWrapper,
    protected @JvmField val onboardingPreferences: OnboardingPreferences,
    protected @JvmField val preferences: LockListPreferences,
    protected @JvmField val cacheInteractor: LockListCacheInteractor) {

  @CheckResult fun populateList(forceRefresh: Boolean): Observable<AppEntry> {
    return Single.defer {
      val dataSource: Single<MutableList<AppEntry>>
      val cache = cacheInteractor.retrieve()
      if (cache == null || forceRefresh) {
        Timber.d("Refresh into cache")
        dataSource = fetchFreshData().cache()
        cacheInteractor.cache(dataSource)
      } else {
        Timber.d("Fetch from cache")
        dataSource = cache
      }
      return@defer dataSource
    }.toObservable().concatMap { Observable.fromIterable(it) }
  }

  @CheckResult protected fun fetchFreshData(): Single<MutableList<AppEntry>> {
    return getActiveApplications().withLatestFrom(isSystemVisible().toObservable(),
        BiFunction<ApplicationInfo, Boolean, Optional<ApplicationInfo>> { application, systemVisible ->
          if (systemVisible) {
            // If system visible, we show all apps
            return@BiFunction Optional.ofNullable(application)
          } else {
            return@BiFunction if (isSystemApplication(application)) Optional.ofNullable(
                null) else Optional.ofNullable(application)
          }
        }).filter { it.isPresent() }
        .flatMapSingle {
          val item: ApplicationInfo = it.item()
          getActivityListForApplication(item).map {
            if (it.isEmpty()) {
              Timber.w("Entry: %s has no activities, hide it", item.packageName)
              return@map ""
            } else {
              return@map item.packageName
            }
          }
        }.filter { it.isNotEmpty() }
        .toList()
        .zipWith(getAppEntryList(),
            BiFunction<List<String>, List<PadLockEntry.AllEntries>, List<LockTuple>> { packageNames, padLockEntries ->
              // Sort here to avoid stream break
              // If the list is empty, the old flatMap call can hang, causing a list loading error
              // Sort here where we are guaranteed a list of some kind
              Collections.sort(padLockEntries) { o1, o2 ->
                o1.packageName().compareTo(o2.packageName(), ignoreCase = true)
              }

              val lockTuples: MutableList<LockTuple> = ArrayList()
              val copyEntries: MutableList<PadLockEntry.AllEntries> = ArrayList(padLockEntries)
              val copyNames: List<String> = ArrayList(packageNames)
              for (packageName in copyNames) {
                var locked = false
                var whitelist = false
                var hardLocked = false
                val removeEntries = HashSet<AllEntries>()
                for (entry in copyEntries) {
                  if (entry.packageName() == packageName) {
                    removeEntries.add(entry)
                    if (entry.activityName() == PadLockEntry.PACKAGE_ACTIVITY_NAME) {
                      locked = true
                    } else if (entry.whitelist()) {
                      whitelist = true
                    } else {
                      hardLocked = true
                    }
                  }
                }
                copyEntries.removeAll(removeEntries)
                lockTuples.add(LockTuple(packageName, locked, whitelist, hardLocked))
              }
              return@BiFunction lockTuples
            }).flatMapObservable { Observable.fromIterable(it) }
        .flatMapMaybe { createFromPackageInfo(it) }
        .toSortedList { o1, o2 -> o1.name().compareTo(o2.name(), ignoreCase = true) }
  }

  @CheckResult protected fun createFromPackageInfo(tuple: LockTuple): Maybe<AppEntry> {
    return packageManager.getApplicationInfo(tuple.packageName)
        .flatMap { info ->
          packageManager.loadPackageLabel(info).map {
            AppEntry.builder()
                .name(it)
                .packageName(tuple.packageName)
                .system(isSystemApplication(info))
                .locked(tuple.locked)
                .build()
          }
        }
  }

  @CheckResult protected fun isSystemApplication(info: ApplicationInfo): Boolean {
    return info.flags and ApplicationInfo.FLAG_SYSTEM != 0
  }

  @CheckResult protected fun getActiveApplications(): Observable<ApplicationInfo> {
    return packageManager.getActiveApplications().flatMapObservable { Observable.fromIterable(it) }
  }

  @CheckResult protected fun getActivityListForApplication(
      info: ApplicationInfo): Single<List<String>> {
    return packageManager.getActivityListForPackage(info.packageName)
  }

  @CheckResult protected fun getAppEntryList(): Single<List<PadLockEntry.AllEntries>> {
    return padLockDB.queryAll()
  }

  @CheckResult fun hasShownOnBoarding(): Single<Boolean> {
    return Single.fromCallable { onboardingPreferences.isListOnBoard() }
  }

  @CheckResult fun isSystemVisible(): Single<Boolean> {
    return Single.fromCallable { preferences.isSystemVisible() }
  }

  fun setSystemVisible(visible: Boolean) {
    preferences.setSystemVisible(visible)
  }

  internal data class LockTuple internal constructor(internal val packageName: String,
      internal val locked: Boolean, internal val whitelist: Boolean,
      internal val hardLocked: Boolean)
}

