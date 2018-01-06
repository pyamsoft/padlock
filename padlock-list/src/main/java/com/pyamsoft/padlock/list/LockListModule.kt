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

package com.pyamsoft.padlock.list

import com.pyamsoft.padlock.api.LockListInteractor
import com.pyamsoft.padlock.api.LockListUpdater
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.data.Cache
import dagger.Binds
import dagger.Module
import javax.inject.Named

@Module
abstract class LockListModule {

    @Binds internal abstract fun provideBus(bus: LockListBus): EventBus<LockListEvent>

    @Binds
    internal abstract fun provideInteractorCache(impl: LockListInteractorCache): LockListInteractor

    @Binds
    @Named("interactor_lock_list")
    internal abstract fun provideInteractor(impl: LockListInteractorImpl): LockListInteractor

    @Binds
    @Named("cache_lock_list")
    internal abstract fun provideCache(cache: LockListInteractorCache): Cache

    @Binds
    internal abstract fun provideUpdater(cache: LockListInteractorCache): LockListUpdater
}
