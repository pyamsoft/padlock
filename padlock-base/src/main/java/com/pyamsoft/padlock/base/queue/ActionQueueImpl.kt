/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.base.queue

import com.pyamsoft.pydroid.helper.clear
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class ActionQueueImpl @Inject internal constructor(
    private val ioScheduler: Scheduler, private val mainThreadScheduler: Scheduler) : ActionQueue {

  private val compositeDisposable: CompositeDisposable = CompositeDisposable()
  private var intervalDisposable: Disposable = null.clear()

  init {
    disposeCurrentInterval()
  }

  override fun queue(disposable: Disposable) {
    restartInterval()

    // Add to the queue
    Timber.d("Add action to ActionQueue...")
    compositeDisposable.add(disposable)
  }

  private fun disposeCurrentInterval() {
    // Always start disposed
    if (intervalDisposable.isDisposed.not()) {
      intervalDisposable = intervalDisposable.clear()
    }
  }

  /**
   * Restarts the interval and counts down again from 1 minute
   *
   * This ensures that when new actions are queued, they are not instantly disposed because of the
   * interval hitting its one minute point
   *
   * Be careful when using this, as a large amount of action can prevent the composite from ever disposing
   */
  private fun restartInterval() {
    disposeCurrentInterval()

    intervalDisposable = Single.timer(1L, MINUTES)
        .subscribeOn(ioScheduler)
        .observeOn(mainThreadScheduler)
        .subscribe({
          Timber.d("Tick Tock interval disposable")
          compositeDisposable.clear()
        }, {
          Timber.e("Error during intervalDisposable execution")
        })
  }

}

