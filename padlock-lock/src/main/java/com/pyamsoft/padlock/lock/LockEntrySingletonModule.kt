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

package com.pyamsoft.padlock.lock

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockEntrySingletonModule {

  @Binds
  internal abstract fun provideLockPassBus(bus: LockPassBus): EventBus<LockPassEvent>

  @Binds
  internal abstract fun provideInteractorCache(
      impl: LockEntryInteractorCache): LockEntryInteractor

  @Binds
  @Named("interactor_lock_entry")
  internal abstract fun provideInteractor(impl: LockEntryInteractorImpl): LockEntryInteractor

  @Binds
  @Named("cache_lock_entry")
  internal abstract fun provideCache(impl: LockEntryInteractorCache): Cache
}

