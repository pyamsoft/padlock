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

package com.pyamsoft.padlock.app.sql;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import rx.Scheduler;
import rx.schedulers.Schedulers;

final class PadLockDB {

  @NonNull private static final Object lock = new Object();

  @Nullable private static volatile PadLockDB instance = null;

  @NonNull private final BriteDatabase briteDatabase;

  private PadLockDB(final @NonNull Context context, final @NonNull Scheduler dbScheduler) {
    final SqlBrite sqlBrite = SqlBrite.create();
    final PadLockOpenHelper openHelper = new PadLockOpenHelper(context.getApplicationContext());
    briteDatabase = sqlBrite.wrapDatabaseHelper(openHelper, dbScheduler);
  }

  @CheckResult @NonNull static BriteDatabase with(final @NonNull Context context) {
    return with(context, Schedulers.io());
  }

  @SuppressWarnings("ConstantConditions") @CheckResult @NonNull
  static BriteDatabase with(final @NonNull Context context, final @NonNull Scheduler dbScheduler) {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new PadLockDB(context.getApplicationContext(), dbScheduler);
        }
      }
    }

    // With double checking, this singleton should be guaranteed non-null
    if (instance == null) {
      throw new NullPointerException("PadLockDB instance is NULL");
    } else {
      return instance.briteDatabase;
    }
  }
}
