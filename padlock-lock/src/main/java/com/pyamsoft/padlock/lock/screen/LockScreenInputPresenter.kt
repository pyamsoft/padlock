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

package com.pyamsoft.padlock.lock.screen

import com.pyamsoft.padlock.lock.screen.LockScreenInputPresenter.View
import com.pyamsoft.padlock.model.LockScreenType.TYPE_PATTERN
import com.pyamsoft.padlock.model.LockScreenType.TYPE_TEXT
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockScreenInputPresenter @Inject internal constructor(
        private val interactor: LockScreenInteractor,
        @Named("computation") computationScheduler: Scheduler,
        @Named("main") mainScheduler: Scheduler,
        @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<View>(computationScheduler,
        ioScheduler, mainScheduler) {

    override fun onBind(v: View) {
        super.onBind(v)
        initializeLockScreenType(v)
    }

    private fun initializeLockScreenType(v: TypeCallback) {
        dispose {
            interactor.getLockScreenType()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        when (it) {
                            TYPE_PATTERN -> v.onTypePattern()
                            TYPE_TEXT -> v.onTypeText()
                            else -> throw IllegalArgumentException("Invalid enum: $it")
                        }
                    }, {
                        Timber.e(it, "Error initializing lock screen type")
                    })
        }
    }

    interface View : TypeCallback

    interface TypeCallback {

        fun onTypePattern()
        fun onTypeText()
    }
}