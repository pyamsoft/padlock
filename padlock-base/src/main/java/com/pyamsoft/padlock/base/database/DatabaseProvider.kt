/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.base.database

import android.content.Context
import androidx.annotation.CheckResult
import androidx.room.Room
import com.pyamsoft.padlock.api.database.EntryDeleteDao
import com.pyamsoft.padlock.api.database.EntryInsertDao
import com.pyamsoft.padlock.api.database.EntryQueryDao
import com.pyamsoft.padlock.api.database.EntryUpdateDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseProvider {

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDb(context: Context): PadLockDb {
    val appContext = context.applicationContext
    return Room.databaseBuilder(appContext, PadLockDbImpl::class.java, PadLockDb.DATABASE_NAME)
        .build()
        .also { it.migrateFromSqlDelight(context) }
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideQueryDao(db: PadLockDb): EntryQueryDao {
    return db.query()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideInsertDao(db: PadLockDb): EntryInsertDao {
    return db.insert()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDeleteDao(db: PadLockDb): EntryDeleteDao {
    return db.delete()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideUpdateDao(db: PadLockDb): EntryUpdateDao {
    return db.update()
  }

}
