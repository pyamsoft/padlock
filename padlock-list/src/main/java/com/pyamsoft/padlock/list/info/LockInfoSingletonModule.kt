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

package com.pyamsoft.padlock.list.info

import com.pyamsoft.padlock.api.LockInfoInteractor
import com.pyamsoft.padlock.api.LockInfoUpdater
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockInfoSingletonModule {

    @Binds internal abstract fun provideBus(bus: LockInfoBus): EventBus<LockInfoEvent>

    @Binds internal abstract fun provideChangeBus(
            bus: LockInfoChangeBus): EventBus<LockInfoEvent.Callback>

    @Binds
    internal abstract fun provideInteractorCache(impl: LockInfoInteractorCache): LockInfoInteractor

    @Binds
    @Named("interactor_lock_info")
    internal abstract fun provideInteractor(impl: LockInfoInteractorImpl): LockInfoInteractor

    @Binds
    @Named("cache_lock_info")
    internal abstract fun provideCache(cache: LockInfoInteractorCache): Cache

    @Binds
    internal abstract fun provideUpdater(cache: LockInfoInteractorCache): LockInfoUpdater
}
