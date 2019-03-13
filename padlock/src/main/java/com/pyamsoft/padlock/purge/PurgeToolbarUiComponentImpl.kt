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

package com.pyamsoft.padlock.purge

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.purge.PurgeToolbarUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import timber.log.Timber
import javax.inject.Inject

internal class PurgeToolbarUiComponentImpl @Inject internal constructor(
  private val presenter: PurgePresenter,
  private val toolbarPresenter: PurgeToolbarPresenter,
  private val purgeAllPresenter: PurgeAllPresenter,
  private val toolbarView: PurgeToolbarView
) : BaseUiComponent<PurgeToolbarUiComponent.Callback>(),
    PurgeToolbarUiComponent,
    PurgeToolbarPresenter.Callback,
    PurgeAllPresenter.Callback {

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: Callback
  ) {
    owner.doOnDestroy {
      toolbarView.teardown()
      toolbarPresenter.unbind()
      purgeAllPresenter.unbind()
      presenter.unbind()
    }

    toolbarView.inflate(savedInstanceState)
    purgeAllPresenter.bind(this)
    toolbarPresenter.bind(this)
  }

  override fun saveState(outState: Bundle) {
    toolbarView.saveState(outState)
  }

  override fun onDeleteAllRequest() {
    presenter.bind(object : PurgePresenter.Callback {

      override fun onFetchStaleBegin() {
      }

      override fun onFetchStaleSuccess(data: List<String>) {
        callback.showPurgeAllConfirmation(data)
      }

      override fun onFetchStaleError(throwable: Throwable) {
        callback.onPurgeErrorOccurred(throwable)
      }

      override fun onFetchStaleComplete() {
        presenter.unbind()
      }

    })

    presenter.fetchData(true)
  }

  override fun onPurgeAllError(throwable: Throwable) {
    callback.onPurgeErrorOccurred(throwable)
  }

  override fun onAllPurged(stalePackages: List<String>) {
    Timber.d("Purged stale: $stalePackages")
    presenter.fetchData(true)
  }

}