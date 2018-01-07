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

package com.pyamsoft.padlock.base

import android.app.Activity
import android.app.IntentService
import android.content.Context
import android.graphics.drawable.Drawable
import com.pyamsoft.pydroid.PYDroidModule
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderModule
import com.pyamsoft.pydroid.loader.cache.ImageCache
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class PadLockProvider(private val pyDroidModule: PYDroidModule,
        private val loaderModule: LoaderModule,
        private val mainActivityClass: Class<out Activity>,
        private val recheckServiceClass: Class<out IntentService>) {

    private val appIconCache: ImageCache<String, Drawable> = AppIconImageCache()

    @Provides
    internal fun provideContext(): Context = pyDroidModule.provideContext()

    @Provides
    @Named(
            "main_activity")
    internal fun provideMainActivityClass(): Class<out Activity> =
            mainActivityClass

    @Provides
    @Named(
            "recheck")
    internal fun provideRecheckServiceClass(): Class<out IntentService> =
            recheckServiceClass

    @Provides
    @Named(
            "computation")
    internal fun provideComputationScheduler(): Scheduler = pyDroidModule.provideComputationScheduler()

    @Provides
    @Named("io")
    internal fun provideIOScheduler(): Scheduler = pyDroidModule.provideIoScheduler()

    @Provides
    @Named("main")
    internal fun provideMainScheduler(): Scheduler = pyDroidModule.provideMainThreadScheduler()

    @Provides
    internal fun provideImageLoader(): ImageLoader = loaderModule.provideImageLoader()
}
