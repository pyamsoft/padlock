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
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.R
import timber.log.Timber

internal abstract class PatternPinView<C : Any> protected constructor(
  owner: LifecycleOwner,
  parent: ViewGroup,
  callback: C,
  isConfirmMode: Boolean,
  @ColorRes private val normalDotColor: Int
) : BasePinView<C>(owner, parent, callback, isConfirmMode) {

  private val layoutRoot by lazyView<ViewGroup>(R.id.pin_pattern_root)
  private val lockView by lazyView<PatternLockView>(R.id.pin_pattern_lock)

  private var lockListener: PatternLockViewListener? = null

  private var isRepeating: Boolean = false
  private var cellPattern = ""
  private var repeatCellPattern = ""

  override val layout: Int = R.layout.layout_pin_pattern

  override val snackbarRoot: View
    get() = layoutRoot

  override fun id(): Int {
    return layoutRoot.id
  }

  override fun teardown() {
    super.teardown()
    lockListener?.also { lockView.removePatternLockListener(it) }
    lockListener = null
  }

  override fun clearDisplay() {
    lockView.clearPattern()
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    setupLockView()
    restoreState(savedInstanceState)
  }

  @CheckResult
  protected fun isRepeating(): Boolean {
    return isRepeating
  }

  private fun restoreState(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) {
      repeatCellPattern = ""
      cellPattern = ""
      isRepeating = false
    } else {
      repeatCellPattern = savedInstanceState.getString(REPEAT_CELL_PATTERN, "")
      cellPattern = savedInstanceState.getString(CELL_PATTERN, "")
      isRepeating = savedInstanceState.getBoolean(REPEATING, false)
    }
  }

  override fun saveState(outState: Bundle) {
    outState.putString(CELL_PATTERN, cellPattern)
    outState.putString(REPEAT_CELL_PATTERN, repeatCellPattern)
    outState.putBoolean(REPEATING, isRepeating())
  }

  private fun clearPattern() {
    if (isRepeating()) {
      Timber.d("Clearing REPEAT pattern")
      repeatCellPattern = ""
    } else {
      Timber.d("Clearing CELL pattern")
      cellPattern = ""
    }
  }

  protected fun commitAndPromptRepeat() {
    isRepeating = true
    promptPatternRepeat()
    clearDisplay()
  }

  private fun setupLockView() {
    lockView.isTactileFeedbackEnabled = false
    lockView.normalStateColor = ContextCompat.getColor(layoutRoot.context, normalDotColor)

    lockListener?.also { lockView.removePatternLockListener(it) }
    val listener = object : PatternLockViewListener {

      override fun onComplete(pattern: List<PatternLockView.Dot>) {
        if (!isRepeating()) {
          if (pattern.size < MINIMUM_PATTERN_LENGTH) {
            setPatternWrong()
          }
        }

        val patternAsString = cellPatternToString(pattern)
        if (isRepeating()) {
          Timber.d("Entered REPEAT pattern")
          repeatCellPattern = patternAsString
        } else {
          Timber.d("Entered CELL pattern")
          cellPattern = patternAsString
        }

        if (isConfirmMode) {
          // Auto fire submit in confirm mode
          submit()
        }
      }

      override fun onCleared() {
      }

      override fun onStarted() {
        setPatternCorrect()
        clearPattern()
      }

      override fun onProgress(progressPattern: MutableList<Dot>?) {
      }
    }

    lockView.addPatternLockListener(listener)
    lockListener = listener
  }

  @CheckResult
  private fun cellPatternToString(cells: List<PatternLockView.Dot>): String {
    val builder = StringBuilder(cells.size)
    cells.map { "${it.row}${it.column}" }
        .forEach { builder.append(it) }
    return builder.toString()
  }

  private fun setPatternCorrect() {
    lockView.setViewMode(PatternLockView.PatternViewMode.CORRECT)
  }

  private fun setPatternWrong() {
    lockView.setViewMode(PatternLockView.PatternViewMode.WRONG)
  }

  private fun promptPatternRepeat() {
    showMessage("Please confirm pattern")
  }

  override fun getAttempt(): String {
    return cellPattern
  }

  override fun getReConfirmAttempt(): String {
    return repeatCellPattern
  }

  override fun getOptionalHint(): String {
    // No hints for Pattern
    return ""
  }

  private fun setEnabled(enable: Boolean) {
    lockView.isEnabled = enable
  }

  override fun enable() {
    setEnabled(true)
  }

  override fun disable() {
    setEnabled(false)
  }

  companion object {

    // NOTE: Cannot be protected yet because of a Kotlin limitation
    internal const val MINIMUM_PATTERN_LENGTH = 4

    private const val REPEAT_CELL_PATTERN = "repeat_cell_pattern"
    private const val CELL_PATTERN = "cell_pattern"
    private const val REPEATING = "repeating"
  }

}