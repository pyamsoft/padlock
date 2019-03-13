/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.api.lockscreen.LockScreenInteractor
import com.pyamsoft.padlock.lock.LockScreenPresenterImpl.CloseOldEvent
import com.pyamsoft.padlock.pin.ConfirmPinView
import com.pyamsoft.padlock.scopes.FragmentScope
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@FragmentScope
internal class LockScreenPresenterImpl @Inject internal constructor(
  private val interactor: LockScreenInteractor,
  bus: EventBus<CloseOldEvent>,
  @Named("locked_package_name") private val packageName: String,
  @Named("locked_activity_name") private val activityName: String,
  @Named("locked_real_name") private val realName: String
) : BasePresenter<CloseOldEvent, LockScreenPresenter.Callback>(bus),
    ConfirmPinView.Callback,
    LockScreenPresenter {

  private var submitDisposable by singleDisposable()
  private var hintDisposable by singleDisposable()
  private var alreadyUnlockedDisposable by singleDisposable()

  override fun onBind() {
    attemptCloseOld()
    loadDisplayName()
  }

  private fun attemptCloseOld() {
    Timber.d("Publish: $packageName $activityName before we listen so we don't close ourselves")

    // If any old listener is present, they would already be subscribed and receive the event
    publish(CloseOldEvent(packageName, activityName))

    // Now listen for any broadcasts from other presenter instances
    listen()
        .filter { it.packageName == packageName }
        .filter { it.activityName == activityName }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onCloseOld() }
        .destroy()
  }

  private fun loadDisplayName() {
    interactor.getDisplayName(packageName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer { callback.onDisplayNameLoaded(it) })
        .destroy()
  }

  override fun onUnbind() {
    submitDisposable.tryDispose()
    alreadyUnlockedDisposable.tryDispose()
    hintDisposable.tryDispose()
  }

  override fun onSubmit(attempt: String) {
    callback.onSubmitUnlockAttempt(attempt)
  }

  override fun checkUnlocked() {
    alreadyUnlockedDisposable = interactor.isAlreadyUnlocked(packageName, activityName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { callback.onAlreadyUnlocked() }
  }

  override fun submit(
    lockCode: String?,
    currentAttempt: String,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long
  ) {
    submitDisposable = interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
        .flatMapCompletable { processSubmission(it, lockCode, isSystem, shouldExclude, ignoreTime) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { callback.onSubmitBegin() }
        .subscribe({ Timber.d("Submit processing...") }, {
          Timber.e(it, "Error submitting unlock attempt")
          callback.onSubmitError(it)
        })
  }

  private fun processSubmission(
    unlocked: Boolean,
    lockCode: String?,
    system: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable {
    Timber.d("Processing submission. Unlocked: $unlocked")
    val completable: Completable
    if (unlocked) {
      completable = postUnlock(lockCode, system, whitelist, ignoreTime)
    } else {
      completable = handleUnlockFailure()
    }

    return completable
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
  }

  @CheckResult
  private fun postUnlock(
    lockCode: String?,
    system: Boolean,
    whitelist: Boolean,
    ignoreTime: Long
  ): Completable {
    return interactor.postUnlock(
        packageName, activityName, realName,
        lockCode, system, whitelist, ignoreTime
    )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { Timber.e(it, "Error on postUnlock") }
        .doOnComplete { callback.onSubmitUnlocked() }
  }

  @CheckResult
  private fun handleUnlockFailure(): Completable {
    return interactor.lockOnFailure(packageName, activityName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError { Timber.e(it, "Error on handleUnlockFailure") }
        .doOnSuccess { lock ->
          if (lock) {
            callback.onSubmitLocked()
          } else {
            callback.onSubmitFailed()
          }
        }
        .ignoreElement()
  }

  override fun displayHint() {
    hintDisposable = interactor.getHint()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ callback.onShowLockHint(it) }, {
          Timber.e(it, "Error displaying hint")
        })
  }

  internal data class CloseOldEvent(
    val packageName: String,
    val activityName: String
  )

}