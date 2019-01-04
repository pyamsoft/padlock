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
import com.pyamsoft.padlock.api.lockscreen.LockEntryInteractor
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Named

class LockViewModel @Inject internal constructor(
  private val enforcer: Enforcer,
  private val interactor: LockEntryInteractor,
  @param:Named("locked_package_name") private val packageName: String,
  @param:Named("locked_activity_name") private val activityName: String,
  @param:Named("locked_real_name") private val realName: String
) {

  @CheckResult
  fun displayLockedHint(onDisplayHint: (hint: String) -> Unit): Disposable {
    return interactor.getHint()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onDisplayHint)
  }

  @CheckResult
  private fun submitPin(
    packageName: String,
    activityName: String,
    lockCode: String?,
    currentAttempt: String,
    onSubmitSuccess: () -> Unit,
    onSubmitFailure: () -> Unit
  ): Single<Boolean> {
    return Single.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.submitPin(packageName, activityName, lockCode, currentAttempt)
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
          if (it) {
            onSubmitSuccess()
          } else {
            onSubmitFailure()
          }
        }
  }

  @CheckResult
  private fun postUnlock(
    lockCode: String?,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long,
    onSubmitResultPostUnlock: () -> Unit
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
        .doOnComplete { onSubmitResultPostUnlock() }
        .andThen(Single.just(Unit))
  }

  @CheckResult
  private fun tryLockEntry(onSubmitResultAttemptLock: () -> Unit): Single<Unit> {
    return Maybe.defer {
      enforcer.assertNotOnMainThread()
      return@defer interactor.lockEntryOnFail(packageName, activityName)
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess {
          if (System.currentTimeMillis() < it) {
            onSubmitResultAttemptLock()
          }
        }
        .map { Unit }
        .toSingle(Unit)
  }

  @CheckResult
  private fun processSubmit(
    lockCode: String?,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long,
    success: Boolean,
    onSubmitResultPostUnlock: () -> Unit,
    onSubmitResultAttemptLock: () -> Unit
  ): Single<Boolean> {
    enforcer.assertNotOnMainThread()
    return Single.defer {
      if (success) {
        return@defer postUnlock(
            lockCode, isSystem, shouldExclude,
            ignoreTime, onSubmitResultPostUnlock
        )
      } else {
        return@defer tryLockEntry(onSubmitResultAttemptLock)
      }
    }
        .map { success }
  }

  @CheckResult
  fun submit(
    lockCode: String?,
    currentAttempt: String,
    isSystem: Boolean,
    shouldExclude: Boolean,
    ignoreTime: Long,
    onSubmitSuccess: () -> Unit,
    onSubmitFailure: () -> Unit,
    onSubmitResultPostUnlock: () -> Unit,
    onSubmitResultAttemptLock: () -> Unit
  ): Disposable {
    return submitPin(
        packageName,
        activityName,
        lockCode,
        currentAttempt,
        onSubmitSuccess,
        onSubmitFailure
    )
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMap {
          processSubmit(
              lockCode, isSystem, shouldExclude, ignoreTime, it,
              onSubmitResultPostUnlock, onSubmitResultAttemptLock
          )
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe()
  }
}
