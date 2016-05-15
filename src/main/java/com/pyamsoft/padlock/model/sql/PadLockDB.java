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

import android.content.Context;
import android.support.annotation.NonNull;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import rx.schedulers.Schedulers;

public class PadLockDB {

  @NonNull private static final Object lock = new Object();

  private static volatile PadLockDB instance = null;

  @NonNull private final BriteDatabase briteDatabase;

  private PadLockDB(final Context context) {
    final SqlBrite sqlBrite = SqlBrite.create();
    final PadLockOpenHelper openHelper = new PadLockOpenHelper(context.getApplicationContext());
    briteDatabase = sqlBrite.wrapDatabaseHelper(openHelper, Schedulers.io());
  }

  @NonNull static BriteDatabase with(final Context context) {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new PadLockDB(context.getApplicationContext());
        }
      }
    }

    // With double checking, this singleton should be guaranteed non-null
    if (instance == null) {
      throw new NullPointerException("PadLockDB instance is NULL");
    }

    return instance.briteDatabase;
  }
}
