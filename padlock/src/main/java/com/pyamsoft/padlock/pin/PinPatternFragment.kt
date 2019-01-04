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

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.andrognito.patternlockview.PatternLockView
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.helper.cellPatternToString
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PinPatternFragment : PinBaseFragment() {

  @field:Inject internal lateinit var pinView: PinPatternView

  private val cellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private val repeatCellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private var nextButtonOnClickRunnable: (() -> Unit)? = null
  private var patternText = ""
  private var repeatPattern = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
    Injector.obtain<PadLockComponent>(requireActivity().applicationContext)
        .plusPinComponent()
        .owner(viewLifecycleOwner)
        .inflater(inflater)
        .container(container)
        .savedInstanceState(savedInstanceState)
        .build()
        .inject(this)

    super.onCreateView(inflater, container, savedInstanceState)

    pinView.create()

    return pinView.root()
  }

  private fun clearPattern() {
    if (repeatPattern) {
      repeatCellPattern.clear()
    } else {
      cellPattern.clear()
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    pinView.onPatternEntry(
        onPattern = {
          if (!repeatPattern) {
            if (it.size < MINIMUM_PATTERN_LENGTH) {
              pinView.setPatternWrong()
            }
          }

          Timber.d("onPatternDetected")
          val cellList: MutableList<PatternLockView.Dot>
          if (repeatPattern) {
            cellList = repeatCellPattern
          } else {
            cellList = cellPattern
          }
          cellList.clear()
          cellList.addAll(it)
        },
        onClear = { clearPattern() }
    )

    checkMasterPin()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(REPEAT_CELL_PATTERN, repeatPattern)
    outState.putString(PATTERN_TEXT, patternText)
    super.onSaveInstanceState(outState)
  }

  override fun clearDisplay() {
    pinView.clearDisplay()
  }

  override fun onMasterPinMissing() {
    nextButtonOnClickRunnable = {
      if (repeatPattern) {
        Timber.d("Submit repeat attempt")
        // Submit
        val repeatText = cellPatternToString(repeatCellPattern)
        submitPin(repeatText)
      } else {
        // process and show next
        if (cellPattern.size < MINIMUM_PATTERN_LENGTH) {
          Timber.d("Pattern is not long enough")
          pinView.infoPatternNotLongEnough()
        } else {
          Timber.d("Submit initial attempt")
          patternText = cellPatternToString(cellPattern)
          repeatPattern = true
          pinView.infoPatternNeedsRepeat()
        }
      }
    }
  }

  override fun onMasterPinPresent() {
    nextButtonOnClickRunnable = {
      patternText = cellPatternToString(cellPattern)
      pinView.clearDisplay()
      submitPin("")
    }
  }

  override fun onSubmitPressed() {
    nextButtonOnClickRunnable?.let {
      Timber.d("Next button pressed, store pattern for re-entry")
      it()
    }
  }

  override fun onInvalidPin() {
    pinView.onInvalidPin()
  }

  private fun submitPin(repeatText: String) {
    // Hint is blank for PIN code
    submitPin(patternText, repeatText, "")
  }

  companion object {

    internal const val TAG = "PinPatternFragment"
    private const val REPEAT_CELL_PATTERN = "repeat_cell_pattern"
    private const val PATTERN_TEXT = "pattern_text"
    @JvmField
    internal var MINIMUM_PATTERN_LENGTH = 4

    @CheckResult
    @JvmStatic
    fun newInstance(checkOnly: Boolean): PinPatternFragment {
      return PinPatternFragment().apply {
        arguments = Bundle().apply {
          putBoolean(PinDialog.CHECK_ONLY, checkOnly)
        }
      }
    }
  }
}
