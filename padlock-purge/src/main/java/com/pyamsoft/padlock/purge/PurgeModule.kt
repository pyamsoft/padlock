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

package com.pyamsoft.padlock.purge

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockDBDelete
import com.pyamsoft.padlock.base.db.PadLockDBQuery
import com.pyamsoft.padlock.base.wrapper.PackageApplicationManager
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class PurgeModule {

  @Singleton
  @Provides
  @CheckResult internal fun providePurgeBus(): EventBus<PurgeEvent> =
      PurgeBus()

  @Singleton
  @Provides
  @CheckResult internal fun providePurgeAllBus(): EventBus<PurgeAllEvent> =
      PurgeAllBus()

  @Singleton
  @Provides
  @CheckResult internal fun providePurgeInteractorCache(
      applicationManager: PackageApplicationManager, deleteDb: PadLockDBDelete,
      queryDb: PadLockDBQuery): PurgeInteractorCache =
      PurgeInteractorCache(PurgeInteractorImpl(applicationManager, deleteDb, queryDb))

  @Singleton
  @Provides
  @CheckResult internal fun providePurgeInteractor(
      cache: PurgeInteractorCache): PurgeInteractor = cache

  @Singleton
  @Provides
  @CheckResult
  @Named("cache_purge") internal fun provideCache(
      cache: PurgeInteractorCache): Cache = cache
}

