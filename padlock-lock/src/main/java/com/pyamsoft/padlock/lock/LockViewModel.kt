/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.api.lockscreen.LockEntryInteractor
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.LOCKED
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.POSTED
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.SUBMIT_FAILURE
import com.pyamsoft.padlock.lock.LockViewModel.LockEntryStage.SUBMIT_SUCCESS
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.core.tryDispose
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import com.pyamsoft.pydroid.core.viewmodel.DataBus
import com.pyamsoft.pydroid.core.viewmodel.DataWrapper
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LockViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val enforcer: Enforcer,
  private val interactor: LockEntryInteractor,
  @param:Named("package_name") private val packageName: String,
  @param:Named("activity_name") private val activityName: String,
  @param:Named("real_name") private val realName: String
) : BaseViewModel(owner) {

  private val lockStageBus = DataBus<LockEntryStage>()
  private val hintBus = RxBus.create<String>()

  private var stageDisposable by singleDisposable()
  private var hintDisposable by singleDisposable()

  override fun onCleared() {
    super.onCleared()
    hintDisposable.tryDispose()
    stageDisposable.tryDispose()
  }

  fun onHintDisplay(func: (String) -> Unit) {
    dispose {
      hintBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  private fun displayLockedHint() {
    hintDisposable = interactor.getHint()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(Consumer { hintBus.publish(it) })
  }

  fun onLockStageBusEvent(func: (DataWrapper<LockEntryStage>) -> Unit) {
    dispose {
      lockStageBus.listen()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(func)
    }
  }

  @CheckResult
  private fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
          if (it) {
            lockStageBus.publishSuccess(SUBMIT_SUCCESS)
          } else {
            displayLockedHint()
            lockStageBus.publishSuccess(SUBMIT_FAILURE)
          }
        }
  }

  @CheckResult
  private fun postUnlock(
    lockCode: String?,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long
  ): Single<Unit> {
    return Completable.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.postUnlock(
          packageName, activityName, realName,
          lockCode, isSystem, shouldExclude, ignoreTime
      )
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete { lockStageBus.publishSuccess(POSTED) }
        .andThen(Single.just(Unit))
  }

  @CheckResult
  private fun tryLockEntry(): Single<Unit> {
    return Maybe.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.lockEntryOnFail(packageName, activityName)
    }
        .doOnSuccess {
          if (System.currentTimeMillis() < it) {
            lockStageBus.publishSuccess(LOCKED)
          }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map { Unit }
        .toSingle(Unit)
  }

  fun submit(
    lockCode: String?,
    currentAttempt: String,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long
  ) {
    stageDisposable = submitPin(packageName, activityName, lockCode, currentAttempt)
        .observeOn(Schedulers.io())
        .flatMap {
          enforcer.assertNotOnMainThread()
          if (it) {
            return@flatMap postUnlock(lockCode, isSystem, shouldExclude, ignoreTime)
          } else {
            return@flatMap tryLockEntry()
          }
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { lockStageBus.publishLoading(false) }
        .doAfterTerminate { lockStageBus.publishComplete() }
        .subscribe({}, {
          Timber.e(it, "Error occurred during submit chain")
          lockStageBus.publishError(it)
        })
  }

  enum class LockEntryStage {
    SUBMIT_SUCCESS,
    SUBMIT_FAILURE,
    LOCKED,
    POSTED
  }
}
