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

import android.os.Bundle
import android.support.annotation.CheckResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.padlock.helper.LockCellUtil
import com.pyamsoft.padlock.list.ErrorDialog
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenPatternFragment : LockScreenBaseFragment(), LockEntryPresenter.View {

  private lateinit var binding: FragmentLockScreenPatternBinding
  @field:Inject
  internal lateinit var presenter: LockEntryPresenter
  private var listener: PatternLockViewListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(context!!.applicationContext)
        .plusLockScreenComponent(
            LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName)
        )
        .inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
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

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    listener = object : PatternLockViewListener {
      override fun onStarted() {
      }

      override fun onProgress(list: List<PatternLockView.Dot>) {
      }

      override fun onComplete(list: List<PatternLockView.Dot>) {
        presenter.submit(lockedCode, LockCellUtil.cellPatternToString(list))
        binding.patternLock.clearPattern()
      }

      override fun onCleared() {
      }
    }

    binding.patternLock.isTactileFeedbackEnabled = false
    binding.patternLock.addPatternLockListener(listener)

    presenter.bind(viewLifecycle, this)
  }

  override fun onSubmitSuccess() {
    Timber.d("Unlocked!")
    presenter.postUnlock(lockedCode, isLockedSystem, isExcluded, selectedIgnoreTime)
  }

  override fun onSubmitFailure() {
    Timber.e("Failed to unlock")
    showSnackbarWithText("Error: Invalid PIN")

    // Once fail count is tripped once, continue to update it every time following until time elapses
    presenter.lockEntry()
  }

  override fun onSubmitError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "submit_error")
  }

  override fun onUnlockError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "unlock_error")
  }

  override fun onLocked() {
    showSnackbarWithText("This entry is temporarily locked")
  }

  override fun onLockedError(throwable: Throwable) {
    DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "locked_error")
  }

  override fun onDisplayHint(hint: String) {
    // No hints for pattern fragment
  }

  override fun onStart() {
    super.onStart()
    binding.patternLock.clearPattern()
  }

  companion object {

    const val TAG = "LockScreenPatternFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(
        lockedPackageName: String,
        lockedActivityName: String,
        lockedCode: String?,
        lockedRealName: String,
        lockedSystem: Boolean
    ): LockScreenPatternFragment {
      val fragment = LockScreenPatternFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(
          lockedPackageName,
          lockedActivityName,
          lockedCode, lockedRealName, lockedSystem
      )
      return fragment
    }
  }
}
