/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import com.pyamsoft.padlock.base.Excludes
import com.pyamsoft.padlock.base.ext.isSystemApplication
import com.pyamsoft.padlock.base.preference.LockListPreferences
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager.ApplicationItem
import com.pyamsoft.pydroid.data.Optional.Present
import com.pyamsoft.pydroid.helper.asOptional
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
import javax.inject.Singleton

@Singleton internal class PackageManagerWrapperImpl @Inject internal constructor(
        context: Context,
        private val listPreferences: LockListPreferences) : PackageActivityManager,
        PackageApplicationManager, PackageLabelManager, PackageDrawableManager {

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
                val packageInfo = packageManager.getPackageInfo(packageName,
                        PackageManager.GET_ACTIVITIES)
                packageInfo.activities?.mapTo(activityEntries) { it.name }
            } catch (e: Exception) {
                Timber.e(e, "PackageManager error, return what we have for %s", packageName)
            }
            return@fromCallable activityEntries
        }.flatMapObservable { Observable.fromIterable(it) }
                .filter { !Excludes.isLockScreen(packageName, it) }
                .filter { !Excludes.isPackageExcluded(packageName) }
                .filter { !Excludes.isClassExcluded(it) }
                .toSortedList()

    }

    @CheckResult
    private fun getInstalledApplications(): Observable<ApplicationItem> {
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
        }.flatMapSingle { getApplicationInfo(it) }.filter { it.isEmpty().not() }
    }

    override fun getActiveApplications(): Single<List<ApplicationItem>> {
        return getInstalledApplications().flatMap<ApplicationItem> {
            if (!it.enabled) {
                Timber.i("Application %s is disabled", it.packageName)
                return@flatMap Observable.empty()
            }

            if (it.system && !listPreferences.isSystemVisible()) {
                Timber.i("Hide system application: %s", it.packageName)
                return@flatMap Observable.empty()
            }

            if (Excludes.isPackageExcluded(it.packageName)) {
                Timber.i("Application %s is excluded", it.packageName)
                return@flatMap Observable.empty()
            }

            Timber.d("Successfully processed application: %s", it.packageName)
            return@flatMap Observable.just(it)
        }.toList()
    }

    override fun getApplicationInfo(packageName: String): Single<ApplicationItem> {
        return Single.defer {
            try {
                val info: ApplicationInfo? = packageManager.getApplicationInfo(packageName, 0)
                if (info == null) {
                    return@defer Single.just(ApplicationItem.EMPTY)
                } else {
                    return@defer Single.just(
                            ApplicationItem(info.packageName, info.isSystemApplication(),
                                    info.enabled))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e, "onError getApplicationInfo: '$packageName'")
                return@defer Single.just(ApplicationItem.EMPTY)
            }
        }
    }

    override fun loadPackageLabel(packageName: String): Single<String> {
        return Single.defer {
            try {
                return@defer Single.just(
                        packageManager.getApplicationInfo(packageName, 0).asOptional())
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.e(e, "onError loadPackageLabel: '$packageName'")
                throw Exceptions.propagate(e)
            }
        }.flatMap {
            if (it is Present) {
                return@flatMap loadPackageLabel(it.value)
            } else {
                return@flatMap Single.just("")
            }
        }
    }

    @CheckResult
    private fun loadPackageLabel(info: ApplicationInfo): Single<String> = Single.fromCallable {
        info.loadLabel(packageManager)?.toString() ?: ""
    }

    override fun isValidActivity(packageName: String, activityName: String): Single<Boolean> {
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