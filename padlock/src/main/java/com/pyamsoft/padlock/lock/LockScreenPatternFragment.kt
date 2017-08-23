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

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.support.annotation.CheckResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.padlock.list.ErrorDialog
import com.pyamsoft.padlock.lock.screen.LockScreenModule
import com.pyamsoft.padlock.uicommon.LockCellUtils
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenPatternFragment : LockScreenBaseFragment() {
  private lateinit var binding: FragmentLockScreenPatternBinding
  @field:Inject internal lateinit var presenter: LockEntryPresenter
  private var listener: PatternLockViewListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.with(context) {
      it.plusLockScreenComponent(
          LockScreenModule(lockedPackageName, lockedActivityName),
          LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName)).inject(this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentLockScreenPatternBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (listener != null) {
      binding.patternLock.removePatternLockListener(listener)
      listener = null
    }
    binding.unbind()
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    listener = object : PatternLockViewListener {
      override fun onStarted() {

      }

      override fun onProgress(list: List<PatternLockView.Dot>) {

      }

      override fun onComplete(list: List<PatternLockView.Dot>) {
        presenter.submit(lockedCode, LockCellUtils.cellPatternToString(list),
            onSubmitSuccess = {
              Timber.d("Unlocked!")
              presenter.postUnlock(lockedCode, isLockedSystem, isExcluded, selectedIgnoreTime,
                  onPostUnlocked = {
                    Timber.d("POST Unlock Finished! 1")
                    presenter.passLockScreen()
                    activity.finish()
                  }, onUnlockError = {
                DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "unlock_error")
              })
            },
            onSubmitFailure = {
              Timber.e("Failed to unlock")
              showSnackbarWithText("Error: Invalid PIN")

              // Once fail count is tripped once, continue to update it every time following until time elapses
              presenter.lockEntry(onLocked = {
                showSnackbarWithText("This entry is temporarily locked")
              }, onLockedError = {
                DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "locked_error")
              })
            },
            onSubmitError = {
              DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "submit_error")
            })
        binding.patternLock.clearPattern()
      }

      override fun onCleared() {

      }
    }

    binding.patternLock.isTactileFeedbackEnabled = false
    binding.patternLock.addPatternLockListener(listener)
  }

  override fun onStart() {
    super.onStart()
    presenter.start(Unit)
    binding.patternLock.clearPattern()
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  companion object {

    internal val TAG = "LockScreenPatternFragment"

    @CheckResult
    fun newInstance(lockedPackageName: String,
        lockedActivityName: String, lockedCode: String?,
        lockedRealName: String, lockedSystem: Boolean): LockScreenPatternFragment {
      val fragment = LockScreenPatternFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(lockedPackageName, lockedActivityName,
          lockedCode, lockedRealName, lockedSystem)
      return fragment
    }
  }
}