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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.padlock.helper.cellPatternToString

class LockScreenPatternFragment : LockScreenBaseFragment() {

  private lateinit var binding: FragmentLockScreenPatternBinding
  private var listener: PatternLockViewListener? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = FragmentLockScreenPatternBinding.inflate(inflater, container, false)

    return binding.root
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
        submitPin(cellPatternToString(list))
        binding.patternLock.clearPattern()
      }

      override fun onCleared() {
      }
    }

    binding.patternLock.isTactileFeedbackEnabled = false
    binding.patternLock.addPatternLockListener(listener)

  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (listener != null) {
      binding.patternLock.removePatternLockListener(listener)
      listener = null
    }
    binding.unbind()
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
  }

  override fun clearDisplay() {
    binding.patternLock.clearPattern()
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
