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

package com.pyamsoft.padlock.service

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.lock.ForegroundEvent
import io.reactivex.Flowable
import io.reactivex.Single

internal interface LockServiceInteractor {

    fun cleanup()

    fun reset()

    fun clearMatchingForegroundEvent(event: ForegroundEvent)

    @CheckResult
    fun isActiveMatching(packageName: String, className: String): Single<Boolean>

    @CheckResult
    fun listenForForegroundEvents(): Flowable<ForegroundEvent>

    @CheckResult
    fun processEvent(packageName: String, className: String,
            forcedRecheck: RecheckStatus): Single<PadLockEntry>
}