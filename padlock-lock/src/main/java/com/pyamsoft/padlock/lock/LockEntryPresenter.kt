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

import com.pyamsoft.padlock.lock.LockEntryPresenter.View
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockEntryPresenter @Inject internal constructor(private val bus: EventBus<LockPassEvent>,
        @param:Named("package_name") private val packageName: String, @param:Named(
                "activity_name") private val activityName: String, @param:Named(
                "real_name") private val realName: String,
        private val interactor: LockEntryInteractor,
        @Named("computation") computationScheduler: Scheduler, @Named("io") ioScheduler: Scheduler,
        @Named("main") mainScheduler: Scheduler) : SchedulerPresenter<View>(computationScheduler,
        ioScheduler, mainScheduler) {

    fun displayLockedHint() {
        dispose {
            interactor.getHint()
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ view?.onDisplayHint(it) },
                            { Timber.e(it, "onError displayLockedHint") })
        }
    }

    fun passLockScreen() {
        bus.publish(LockPassEvent(packageName, activityName))
    }

    fun submit(lockCode: String?, currentAttempt: String) {
        dispose {
            interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        Timber.d("Received unlock entry result")
                        if (it) {
                            view?.onSubmitSuccess()
                        } else {
                            view?.onSubmitFailure()
                        }
                    }, {
                        Timber.e(it, "unlockEntry onError")
                        view?.onSubmitError(it)
                    })
        }
    }

    fun lockEntry() {
        dispose {
            interactor.lockEntryOnFail(packageName, activityName)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        if (System.currentTimeMillis() < it) {
                            Timber.w("Lock em up")
                            view?.onLocked()
                        }
                    }, {
                        Timber.e(it, "lockEntry onError")
                        view?.onLockedError(it)
                    })
        }
    }

    fun postUnlock(lockCode: String?, isSystem: Boolean, shouldExclude: Boolean, ignoreTime: Long) {
        dispose {
            interactor.postUnlock(packageName, activityName, realName, lockCode, isSystem,
                    shouldExclude, ignoreTime)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        Timber.d("onPostUnlock complete")
                        view?.onPostUnlocked()
                    }, {
                        Timber.e(it, "Error postunlock")
                        view?.onUnlockError(it)
                    })
        }
    }

    interface View : HintCallback, LockCallack, PostUnlockCallback, SubmitCallback

    interface SubmitCallback {
        fun onSubmitSuccess()
        fun onSubmitFailure()
        fun onSubmitError(throwable: Throwable)
    }

    interface PostUnlockCallback {

        fun onPostUnlocked()

        fun onUnlockError(throwable: Throwable)
    }

    interface LockCallack {

        fun onLocked()

        fun onLockedError(throwable: Throwable)

    }

    interface HintCallback {

        fun onDisplayHint(hint: String)
    }
}
