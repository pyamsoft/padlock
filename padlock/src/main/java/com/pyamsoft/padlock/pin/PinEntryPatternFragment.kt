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

package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.support.annotation.CheckResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.databinding.FragmentPinEntryPatternBinding
import com.pyamsoft.padlock.uicommon.LockCellUtils
import com.pyamsoft.pydroid.ui.helper.Toasty
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PinEntryPatternFragment : PinEntryBaseFragment() {

  @field:Inject internal lateinit var presenter: PinEntryPresenter
  private val cellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private val repeatCellPattern: MutableList<PatternLockView.Dot> = ArrayList()
  private var repeatPattern = false
  private lateinit var binding: FragmentPinEntryPatternBinding
  private var nextButtonOnClickRunnable: (() -> Boolean)? = null
  private var patternText = ""
  private var listener: PatternLockViewListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.with(context) {
      it.inject(this)
    }

    if (savedInstanceState == null) {
      repeatPattern = false
      patternText = ""
    } else {
      repeatPattern = savedInstanceState.getBoolean(REPEAT_CELL_PATTERN, false)
      patternText = savedInstanceState.getString(PATTERN_TEXT, "")
    }
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentPinEntryPatternBinding.inflate(inflater, container, false)
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

  override fun onDestroy() {
    super.onDestroy()
    presenter.destroy()
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
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
        val cellList: MutableList<PatternLockView.Dot>
        if (repeatPattern) {
          cellList = repeatCellPattern
        } else {
          cellList = cellPattern
        }
        cellList.clear()
        cellList.addAll(list)
      }

      override fun onCleared() {
        clearPattern()
      }
    }

    binding.patternLock.isTactileFeedbackEnabled = false
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    outState!!.putBoolean(REPEAT_CELL_PATTERN, repeatPattern)
    outState.putString(PATTERN_TEXT, patternText)
    super.onSaveInstanceState(outState)
  }

  override fun onStart() {
    super.onStart()

    // Pattern gets visually screwed up in multiwindow mode, clear it
    binding.patternLock.clearPattern()

    presenter.checkMasterPinPresent(onMasterPinMissing = {
      nextButtonOnClickRunnable = runnable@ {
        if (repeatPattern) {
          // Submit
          val repeatText = LockCellUtils.cellPatternToString(repeatCellPattern)
          submitPin(repeatText)
          // No follow up acton
          return@runnable false
        } else {
          // process and show next
          if (cellPattern.size < MINIMUM_PATTERN_LENGTH) {
            binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG)
            return@runnable false
          } else {
            patternText = LockCellUtils.cellPatternToString(cellPattern)
            repeatPattern = true
            binding.patternLock.clearPattern()
            return@runnable false
          }
        }
      }
    }, onMasterPinPresent = {
      nextButtonOnClickRunnable = runnable@ {
        patternText = LockCellUtils.cellPatternToString(cellPattern)
        binding.patternLock.clearPattern()
        submitPin(null)
        return@runnable false
      }
    })

    binding.patternLock.addPatternLockListener(listener)
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
    if (listener != null) {
      binding.patternLock.removePatternLockListener(listener)
    }
  }

  @CheckResult internal fun onNextButtonClicked(): Boolean {
    val callable: (() -> Boolean)? = nextButtonOnClickRunnable
    if (callable == null) {
      Timber.w("onClick runnable is NULL")
      return false
    } else {
      try {
        return callable()
      } catch (e: Exception) {
        return false
      }

    }
  }

  internal fun submitPin(repeatText: String?) {
    var text = repeatText
    if (text == null) {
      text = ""
    }

    presenter.submit(patternText, text, "", onSubmitSuccess = {
      if (it) {
        presenter.publish(CreatePinEvent.create(true))
      } else {
        presenter.publish(ClearPinEvent.create(true))
      }
      dismissParent()
    }, onSubmitFailure = {
      if (it) {
        presenter.publish(CreatePinEvent.create(false))
      } else {
        presenter.publish(ClearPinEvent.create(false))
      }
      dismissParent()
    }, onSubmitError = {
      Toasty.makeText(context, it.message.toString(), Toasty.LENGTH_SHORT).show()
      dismissParent()
    })
  }

  companion object {

    const internal val TAG = "PinEntryPatternFragment"
    const private val REPEAT_CELL_PATTERN = "repeat_cell_pattern"
    const private val PATTERN_TEXT = "pattern_text"
    internal var MINIMUM_PATTERN_LENGTH = 4
  }
}
