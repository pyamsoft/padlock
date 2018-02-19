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

package com.pyamsoft.padlock.pin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.padlock.helper.LockCellUtil
import com.pyamsoft.pydroid.util.Toasty
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PinEntryPatternFragment : PinEntryBaseFragment(), PinEntryPresenter.View {

  @field:Inject
  internal lateinit var presenter: PinEntryPresenter
  private lateinit var binding: FragmentLockScreenPatternBinding
  private val cellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private val repeatCellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private var nextButtonOnClickRunnable: (() -> Boolean) = { false }
  private var patternText = ""
  private var listener: PatternLockViewListener? = null
  private var repeatPattern = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtain<PadLockComponent>(context!!.applicationContext)
        .inject(this)

    if (savedInstanceState == null) {
      repeatPattern = false
      patternText = ""
    } else {
      repeatPattern = savedInstanceState.getBoolean(REPEAT_CELL_PATTERN, false)
      patternText = savedInstanceState.getString(PATTERN_TEXT, "")
    }
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
    setupLockView()

    listener = object : PatternLockViewListener {

      private fun clearPattern() {
        if (repeatPattern) {
          repeatCellPattern.clear()
        } else {
          cellPattern.clear()
        }
      }

      override fun onStarted() {
        binding.patternLock.setViewMode(PatternLockView.PatternViewMode.CORRECT)
        clearPattern()
      }

      override fun onProgress(list: List<PatternLockView.Dot>) {
      }

      override fun onComplete(list: List<PatternLockView.Dot>) {
        if (!repeatPattern) {
          if (list.size < MINIMUM_PATTERN_LENGTH) {
            binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG)
          }
        }

        Timber.d("onPatternDetected")
        val cellList: MutableList<PatternLockView.Dot> = if (repeatPattern) {
          // Assign to cellList
          repeatCellPattern
        } else {
          // Assign to cellList
          cellPattern
        }

        cellList.clear()
        cellList.addAll(list)
      }

      override fun onCleared() {
        clearPattern()
      }
    }

    binding.patternLock.isTactileFeedbackEnabled = false
    binding.patternLock.addPatternLockListener(listener)

    presenter.bind(viewLifecycle, this)
  }

  private fun setupLockView() {
    // Set the dots to be black so we can see them
    binding.patternLock.normalStateColor = Color.BLACK
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(REPEAT_CELL_PATTERN, repeatPattern)
    outState.putString(PATTERN_TEXT, patternText)
    super.onSaveInstanceState(outState)
  }

  override fun onStart() {
    super.onStart()

    // Pattern gets visually screwed up in multiwindow mode, clear it
    binding.patternLock.clearPattern()
  }

  override fun onMasterPinMissing() {
    nextButtonOnClickRunnable = runnable@ {
      if (repeatPattern) {
        Timber.d("Submit repeat attempt")
        // Submit
        val repeatText = LockCellUtil.cellPatternToString(repeatCellPattern)
        submitPin(repeatText)
        // No follow up acton
        return@runnable false
      } else {
        // process and show next
        if (cellPattern.size < MINIMUM_PATTERN_LENGTH) {
          Timber.d("Pattern is not long enough")
          Toasty.makeText(
              binding.patternLock.context, "Pattern is not long enough",
              Toasty.LENGTH_SHORT
          )
          binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG)
          return@runnable false
        } else {
          Timber.d("Submit initial attempt")
          patternText = LockCellUtil.cellPatternToString(cellPattern)
          repeatPattern = true
          binding.patternLock.clearPattern()
          Toasty.makeText(
              binding.patternLock.context, "Please confirm pattern",
              Toasty.LENGTH_SHORT
          )
          return@runnable false
        }
      }
    }
  }

  override fun onMasterPinPresent() {
    nextButtonOnClickRunnable = runnable@ {
      patternText = LockCellUtil.cellPatternToString(cellPattern)
      binding.patternLock.clearPattern()
      submitPin("")
      return@runnable false
    }
  }

  override fun onSubmitPressed() {
    Timber.d("Next button pressed, store pattern for re-entry")
    nextButtonOnClickRunnable()
  }

  override fun onPinSubmitCreateSuccess() {
    Timber.d("Create success")
  }

  override fun onPinSubmitCreateFailure() {
    Timber.d("Create failure")
  }

  override fun onPinSubmitClearSuccess() {
    Timber.d("Clear success")
  }

  override fun onPinSubmitClearFailure() {
    Timber.d("Clear failure")
  }

  override fun onPinSubmitError(throwable: Throwable) {
    Toasty.makeText(context!!, throwable.message.toString(), Toasty.LENGTH_SHORT)
  }

  override fun onPinSubmitComplete() {
    dismissParent()
  }

  private fun submitPin(repeatText: String) {
    // Hint is blank for PIN code
    presenter.submit(patternText, repeatText, "")
  }

  companion object {

    internal const val TAG = "PinEntryPatternFragment"
    private const val REPEAT_CELL_PATTERN = "repeat_cell_pattern"
    private const val PATTERN_TEXT = "pattern_text"
    @JvmField
    internal var MINIMUM_PATTERN_LENGTH = 4
  }
}
