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

package com.pyamsoft.padlock.base.db

import android.content.Context
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named
import javax.inject.Singleton

@Module
class PadLockDBModule {

  @Singleton
  @Provides internal fun providePadLockDB(context: Context,
      @Named("io") scheduler: Scheduler): PadLockDBImpl =
      PadLockDBImpl(context.applicationContext, scheduler)

  @Singleton
  @Provides internal fun providePadLockInsert(db: PadLockDBImpl): PadLockDBInsert = db

  @Singleton
  @Provides internal fun providePadLockQuery(db: PadLockDBImpl): PadLockDBQuery = db

  @Singleton
  @Provides internal fun providePadLockUpdate(db: PadLockDBImpl): PadLockDBUpdate = db

  @Singleton
  @Provides internal fun providePadLockDelete(db: PadLockDBImpl): PadLockDBDelete = db
}
