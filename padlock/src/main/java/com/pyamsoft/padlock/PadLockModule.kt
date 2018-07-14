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

package com.pyamsoft.padlock

import com.pyamsoft.padlock.api.PadLockDatabaseDelete
import com.pyamsoft.padlock.api.PadLockDatabaseInsert
import com.pyamsoft.padlock.api.PadLockDatabaseQuery
import com.pyamsoft.padlock.api.PadLockDatabaseUpdate
import com.pyamsoft.padlock.db.PadLockDatabaseImpl
import dagger.Binds
import dagger.Module

@Module
abstract class PadLockModule {

  @Binds
  internal abstract fun providePadLockInsert(impl: PadLockDatabaseImpl): PadLockDatabaseInsert

  @Binds
  internal abstract fun providePadLockQuery(impl: PadLockDatabaseImpl): PadLockDatabaseQuery

  @Binds
  internal abstract fun providePadLockUpdate(impl: PadLockDatabaseImpl): PadLockDatabaseUpdate

  @Binds
  internal abstract fun providePadLockDelete(impl: PadLockDatabaseImpl): PadLockDatabaseDelete

}
