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

package com.pyamsoft.padlock.base.db;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import io.reactivex.Flowable;
import java.util.List;

public interface PadLockDB {

  @CheckResult @NonNull Flowable<Long> insert(@NonNull String packageName,
      @NonNull String activityName, @Nullable String lockCode, long lockUntilTime,
      long ignoreUntilTime, boolean isSystem, boolean whitelist);

  @CheckResult @NonNull Flowable<Integer> updateLockTime(long lockUntilTime,
      @NonNull String packageName, @NonNull String activityName);

  @CheckResult @NonNull Flowable<Integer> updateIgnoreTime(long ignoreUntilTime,
      @NonNull String packageName, @NonNull String activityName);

  @CheckResult @NonNull Flowable<Integer> updateWhitelist(boolean whitelist,
      @NonNull String packageName, @NonNull String activityName);

  /**
   * Get either the package with specific name of the PACKAGE entry
   *
   * SQLite only has bindings so we must make do
   * ?1 package name
   * ?2 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?3 the specific activity name
   * ?4 the PadLock PACKAGE_TAG, see model.PadLockEntry
   * ?5 the specific activity name
   */
  @NonNull @CheckResult Flowable<PadLockEntry> queryWithPackageActivityNameDefault(
      @NonNull String packageName, @NonNull String activityName);

  @NonNull @CheckResult Flowable<List<PadLockEntry.WithPackageName>> queryWithPackageName(
      @NonNull String packageName);

  @NonNull @CheckResult Flowable<List<PadLockEntry.AllEntries>> queryAll();

  @NonNull @CheckResult Flowable<Integer> deleteWithPackageName(@NonNull String packageName);

  @NonNull @CheckResult Flowable<Integer> deleteWithPackageActivityName(@NonNull String packageName,
      @NonNull String activityName);

  @NonNull @CheckResult Flowable<Integer> deleteAll();

  @NonNull @CheckResult Flowable<Boolean> deleteDatabase();
}
