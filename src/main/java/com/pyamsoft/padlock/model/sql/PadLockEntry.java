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
package com.pyamsoft.padlock.model.sql;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.squareup.sqlbrite.BriteDatabase;
import java.util.List;
import rx.Observable;

@AutoValue public abstract class PadLockEntry implements PadLockEntryModel {

  @NonNull private static final Mapper<PadLockEntry> MAPPER =
      new Mapper<>(AutoValue_PadLockEntry::new);

  // SQLDelight does not yet support delete strings
  @NonNull private static final String DELETE_WITH_PACKAGE_NAME = "packageName = ?";
  @NonNull private static final String DELETE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";
  @NonNull private static final String DELETE_ALL = "1=1";

  // SQLDelight does not yet support update strings
  @NonNull private static final String UPDATE_WITH_PACKAGE_ACTIVITY_NAME =
      "packageName = ? AND activityName = ?;";

  @SuppressLint("NewApi") public static void newTransaction(final @NonNull Context context,
      final @NonNull Runnable runnable) {
    final Context appContext = context.getApplicationContext();
    try (
        final BriteDatabase.Transaction transaction = PadLockDB.with(appContext).newTransaction()) {
      runnable.run();
      transaction.markSuccessful();
    }
  }

  public static void insert(final @NonNull Context context,
      final @NonNull ContentValues contentValues) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext).insert(TABLE_NAME, contentValues);
  }

  @NonNull @CheckResult public static Observable<PadLockEntry> queryWithPackageActivityName(
      final @NonNull Context context, final @NonNull String packageName,
      final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(TABLE_NAME, WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName)
        .mapToOne(MAPPER::map)
        .filter(padLockEntry -> padLockEntry != null);
  }

  @NonNull @CheckResult
  public static Observable<List<PadLockEntry>> queryWithPackageName(final @NonNull Context context,
      final @NonNull String packageName) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(TABLE_NAME, WITH_PACKAGE_NAME, packageName)
        .mapToList(MAPPER::map)
        .filter(padLockEntries -> padLockEntries != null);
  }

  public static void updateWithPackageActivityName(final @NonNull Context context,
      final @NonNull ContentValues contentValues, final @NonNull String packageName,
      final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext)
        .update(TABLE_NAME, contentValues, UPDATE_WITH_PACKAGE_ACTIVITY_NAME, packageName,
            activityName);
  }

  @NonNull @CheckResult
  public static Observable<List<PadLockEntry>> queryAll(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    return PadLockDB.with(appContext)
        .createQuery(TABLE_NAME, ALL_ENTRIES)
        .mapToList(MAPPER::map)
        .filter(padLockEntries -> padLockEntries != null);
  }

  public static void deleteWithPackageName(final @NonNull Context context,
      final @NonNull String packageName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext).delete(TABLE_NAME, DELETE_WITH_PACKAGE_NAME, packageName);
  }

  public static void deleteWithPackageActivityName(final @NonNull Context context,
      final @NonNull String packageName, final @NonNull String activityName) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext)
        .delete(TABLE_NAME, DELETE_WITH_PACKAGE_ACTIVITY_NAME, packageName, activityName);
  }

  public static void deleteAll(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    PadLockDB.with(appContext).delete(TABLE_NAME, DELETE_ALL);
  }

  // Empty class just to allow Marshal access
  public static final class Marshal extends PadLockEntryMarshal<Marshal> {

  }
}
