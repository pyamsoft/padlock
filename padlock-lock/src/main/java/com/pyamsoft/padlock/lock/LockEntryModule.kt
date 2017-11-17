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

import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class LockEntryModule(private val packageName: String, private val activityName: String,
        private val realName: String) {

    @Provides
    @Named("package_name") internal fun providePackageName(): String = packageName

    @Provides
    @Named("activity_name") internal fun provideActivityName(): String = activityName

    @Provides
    @Named("real_name") internal fun provideRealName(): String = realName
}

