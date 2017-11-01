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

import com.pyamsoft.padlock.base.db.PadLockEntry
import com.pyamsoft.padlock.lock.LockPassEvent
import com.pyamsoft.padlock.service.LockServicePresenter.View
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.helper.clear
import com.pyamsoft.pydroid.presenter.SchedulerPresenter
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockServicePresenter @Inject internal constructor(
    private val lockPassBus: EventBus<LockPassEvent>,
    private val serviceFinishBus: EventBus<ServiceFinishEvent>,
    private val recheckEventBus: EventBus<RecheckEvent>,
    private val interactor: LockServiceInteractor,
    @Named("computation") compScheduler: Scheduler,
    @Named("main") mainScheduler: Scheduler,
    @Named("io") ioScheduler: Scheduler) : SchedulerPresenter<View>(compScheduler,
    ioScheduler,
    mainScheduler) {

  private var matchingDisposable: Disposable = null.clear()
  private var entryDisposable: Disposable = null.clear()

  init {
    interactor.reset()
  }

  override fun onBind(v: View) {
    super.onBind(v)
    registerOnBus(v)
  }

  override fun onUnbind() {
    super.onUnbind()
    interactor.cleanup()
    interactor.reset()

    matchingDisposable = matchingDisposable.clear()
    entryDisposable = entryDisposable.clear()
  }

  private fun registerOnBus(v: BusCallback) {
    dispose {
      serviceFinishBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            v.onFinish()
          }, { Timber.e(it, "onError service finish bus") })
    }

    dispose {
      lockPassBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            interactor.setLockScreenPassed(it.packageName, it.className, true)
          }, { Timber.e(it, "onError lock passed bus") })
    }

    dispose {
      recheckEventBus.listen()
          .subscribeOn(ioScheduler)
          .observeOn(mainThreadScheduler)
          .subscribe({
            v.onRecheck(it.packageName, it.className)
          }, { Timber.e(it, "onError recheck event bus") })
    }
  }

  fun processActiveApplicationIfMatching(packageName: String, className: String) {
    matchingDisposable = matchingDisposable.clear()
    matchingDisposable = interactor.isActiveMatching(packageName, className)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          if (it) {
            processAccessibilityEvent(packageName, className, RecheckStatus.FORCE)
          }
        }, { Timber.e(it, "onError processActiveApplicationIfMatching") })
  }

  fun processAccessibilityEvent(packageName: String, className: String,
      forcedRecheck: RecheckStatus) {
    entryDisposable = entryDisposable.clear()
    entryDisposable = interactor.processEvent(packageName, className, forcedRecheck)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          if (PadLockEntry.isEmpty(it)) {
            Timber.w("PadLockEntry is EMPTY, ignore")
          } else {
            view?.onStartLockScreen(it, className)
          }
        }, {
          if (it is NoSuchElementException) {
            Timber.w("PadLock not locking: $packageName, $className")
          } else {
            Timber.e(it, "Error getting PadLockEntry for LockScreen")
          }
        })
  }

  interface View : BusCallback, LockScreenCallback

  interface LockScreenCallback {

    fun onStartLockScreen(entry: PadLockEntry, realName: String)
  }

  interface BusCallback {

    fun onFinish()

    fun onRecheck(packageName: String, className: String)
  }
}
