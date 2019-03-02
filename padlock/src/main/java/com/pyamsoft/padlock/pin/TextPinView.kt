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
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import timber.log.Timber

internal abstract class TextPinView<C : Any> protected constructor(
  owner: LifecycleOwner,
  parent: ViewGroup,
  callback: C,
  isConfirmMode: Boolean
) : BasePinView<C>(owner, parent, callback, isConfirmMode) {

  private val attemptLayout by lazyView<TextInputLayout>(R.id.pin_text_attempt)
  private val reConfirmAttemptLayout by lazyView<TextInputLayout>(R.id.pin_text_reconfirm_attempt)
  private val optionalHintLayout by lazyView<TextInputLayout>(R.id.pin_text_optional_hint)
  private val confirmButton by lazyView<TextInputLayout>(R.id.pin_text_confirm)
  private val showHint by lazyView<TextView>(R.id.pin_text_show_hint)

  override val layoutRoot by lazyView<ScrollView>(R.id.pin_text_root)

  override val layout: Int = R.layout.layout_pin_text

  override fun id(): Int {
    return layoutRoot.id
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    showViews()
    bindClicks()
    restoreState(savedInstanceState)
    requireEditText(attemptLayout).requestFocus()
  }

  @CheckResult
  private fun requireEditText(layout: TextInputLayout): EditText {
    return requireNotNull(layout.editText)
  }

  override fun clearDisplay() {
    requireEditText(attemptLayout).setText("")
    requireEditText(reConfirmAttemptLayout).setText("")
    requireEditText(optionalHintLayout).setText("")
  }

  override fun saveState(outState: Bundle) {
    super.saveState(outState)
    outState.putString(CODE_DISPLAY, getAttempt())
    outState.putString(CODE_REENTRY_DISPLAY, getReConfirmAttempt())
    outState.putString(HINT_DISPLAY, getOptionalHint())
  }

  private fun restoreState(state: Bundle?) {
    if (state == null) {
      return
    }

    val attempt = requireNotNull(state.getString(CODE_DISPLAY, ""))
    val reEntry = requireNotNull(state.getString(CODE_REENTRY_DISPLAY, ""))
    val hint = requireNotNull(state.getString(HINT_DISPLAY, ""))

    if (attempt.isBlank() && reEntry.isBlank() && hint.isBlank()) {
      clearDisplay()
    } else {
      requireEditText(attemptLayout).setText(attempt)
      requireEditText(reConfirmAttemptLayout).setText(reEntry)
      requireEditText(optionalHintLayout).setText(hint)
    }
  }

  override fun teardown() {
    confirmButton.setOnDebouncedClickListener(null)
    requireEditText(attemptLayout).setOnEditorActionListener(null)
    requireEditText(reConfirmAttemptLayout).setOnEditorActionListener(null)
    requireEditText(optionalHintLayout).setOnEditorActionListener(null)
  }

  private fun bindClicks() {
    confirmButton.setOnDebouncedClickListener { submit() }

    requireEditText(attemptLayout).also { editView ->
      setupSubmissionView(editView) {
        if (reConfirmAttemptLayout.isVisible) {
          reConfirmAttemptLayout.requestFocus()
        } else {
          submit()
        }
      }
    }

    requireEditText(reConfirmAttemptLayout).also { editView ->
      setupSubmissionView(editView) {
        if (optionalHintLayout.isVisible) {
          optionalHintLayout.requestFocus()
        } else {
          submit()
        }
      }
    }

    requireEditText(optionalHintLayout).also { editView ->
      setupSubmissionView(editView) {
        submit()
      }
    }
  }

  final override fun getAttempt(): String {
    return requireEditText(attemptLayout).text.toString()
  }

  final override fun getReConfirmAttempt(): String {
    return requireEditText(reConfirmAttemptLayout).text.toString()
  }

  final override fun getOptionalHint(): String {
    return requireEditText(optionalHintLayout).text.toString()
  }

  private fun showViews() {
    if (isConfirmMode) {
      attemptLayout.isVisible = true
      showHint.isVisible = true
      confirmButton.isVisible = true
      reConfirmAttemptLayout.isGone = true
      optionalHintLayout.isGone = true
    } else {
      attemptLayout.isVisible = true
      reConfirmAttemptLayout.isVisible = true
      optionalHintLayout.isVisible = true
      showHint.isGone = true
      confirmButton.isGone = true
    }
  }

  private inline fun setupSubmissionView(
    view: EditText,
    crossinline onEnter: () -> Unit
  ) {
    view.setOnEditorActionListener { _, actionId, keyEvent ->
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress")
        return@setOnEditorActionListener false
      }

      if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed")
        onEnter()
        return@setOnEditorActionListener true
      }

      Timber.d("Do not handle key event")
      return@setOnEditorActionListener false
    }
  }

  private fun setEnabled(enable: Boolean) {
    attemptLayout.isEnabled = enable
    reConfirmAttemptLayout.isEnabled = enable
    optionalHintLayout.isEnabled = enable
    confirmButton.isEnabled = enable
    showHint.isEnabled = enable
  }

  override fun enable() {
    setEnabled(true)
  }

  override fun disable() {
    setEnabled(false)
  }

  override fun showErrorMessage(message: String) {
    Snackbreak.bindTo(owner)
        .short(layoutRoot, message)
        .show()
  }

  protected fun setHintText(hint: String) {
    if (isConfirmMode) {
      showHint.text = hint
    } else {
      showHint.text = ""
    }
  }

  companion object {

    private const val CODE_DISPLAY = "CODE_DISPLAY"
    private const val CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY"
    private const val HINT_DISPLAY = "HINT_DISPLAY"
  }
}

