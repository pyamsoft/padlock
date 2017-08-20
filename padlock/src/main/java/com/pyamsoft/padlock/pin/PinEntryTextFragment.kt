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

import android.content.Context
import android.os.Bundle
import android.support.annotation.CheckResult
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentPinEntryTextBinding
import com.pyamsoft.padlock.pin.PinEntryPresenter.Callback
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.ui.helper.Toasty
import timber.log.Timber
import javax.inject.Inject

class PinEntryTextFragment : PinEntryBaseFragment(), Callback {

  @field:Inject internal lateinit var presenter: PinEntryPresenter
  private lateinit var imm: InputMethodManager
  private lateinit var binding: FragmentPinEntryTextBinding
  private var pinReentryText: EditText? = null
  private var pinEntryText: EditText? = null
  private var pinHintText: EditText? = null
  private var goTask = LoaderHelper.empty()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.with(context) {
      it.inject(this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentPinEntryTextBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    goTask = LoaderHelper.unload(goTask)
    imm.toggleSoftInputFromWindow(activity.window.decorView.windowToken, 0, 0)
    binding.unbind()
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // Resolve TextInputLayout edit texts
    val obj1: EditText? = binding.pinEntryCode.editText
    if (obj1 == null) {
      throw NullPointerException("No item entry edit text")
    } else {
      pinEntryText = obj1
    }

    val obj2 = binding.pinReentryCode.editText
    if (obj2 == null) {
      throw NullPointerException("No item reentry edit text")
    } else {
      pinReentryText = obj2
    }

    val obj3 = binding.pinHint.editText
    if (obj3 == null) {
      throw NullPointerException("No item hint edit text")
    } else {
      pinHintText = obj3
    }

    // Force the keyboard
    imm = context.applicationContext
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)

    clearDisplay()
    setupGoArrow()

    binding.pinImageGo.setOnClickListener {
      submit()
      imm.toggleSoftInputFromWindow(activity.window.decorView.windowToken, 0,
          0)
    }

    if (savedInstanceState != null) {
      onRestoreInstanceState(savedInstanceState)
    }
  }

  private fun setupSubmissionView(view: EditText) {
    view.setOnEditorActionListener { _, actionId, keyEvent ->
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress")
        return@setOnEditorActionListener false
      }

      if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed")
        submit()
        return@setOnEditorActionListener true
      }

      Timber.d("Do not handle key event")
      return@setOnEditorActionListener false
    }
  }

  private fun submit() {
    presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint(), onCreateSuccess = {
      clearDisplay()
      presenter.publish(CreatePinEvent(true))
    }, onCreateFailure = {
      clearDisplay()
      presenter.publish(CreatePinEvent(false))
    }, onClearSuccess = {
      clearDisplay()
      presenter.publish(ClearPinEvent(true))
    }, onClearFailure = {
      clearDisplay()
      presenter.publish(ClearPinEvent(false))
    }, onSubmitError = {
      clearDisplay()
      Toasty.makeText(context, it.message.toString(), Toasty.LENGTH_SHORT).show()
    }, onComplete = {
      dismissParent()
    })
  }

  override fun onStart() {
    super.onStart()
    presenter.start(this)
  }

  override fun onMasterPinMissing() {
    Timber.d("No active master, show extra views")
    binding.pinReentryCode.visibility = View.VISIBLE
    binding.pinHint.visibility = View.VISIBLE
    val obj = pinHintText
    if (obj != null) {
      setupSubmissionView(obj)
    }
  }

  override fun onMasterPinPresent() {
    Timber.d("Active master, hide extra views")
    binding.pinReentryCode.visibility = View.GONE
    binding.pinHint.visibility = View.GONE
    val obj = pinEntryText
    if (obj != null) {
      setupSubmissionView(obj)
    }
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  private fun setupGoArrow() {
    // Force keyboard focus
    pinEntryText?.requestFocus()

    goTask = LoaderHelper.unload(goTask)
    goTask = ImageLoader.fromResource(activity, R.drawable.ic_arrow_forward_24dp)
        .tint(R.color.orangeA200)
        .into(binding.pinImageGo)
  }

  private fun onRestoreInstanceState(savedInstanceState: Bundle) {
    Timber.d("onRestoreInstanceState")
    val attempt = savedInstanceState.getString(CODE_DISPLAY, null)
    val reentry = savedInstanceState.getString(CODE_REENTRY_DISPLAY, null)
    val hint = savedInstanceState.getString(HINT_DISPLAY, null)
    if (attempt == null || reentry == null || hint == null) {
      Timber.d("Empty attempt")
      clearDisplay()
    } else {
      Timber.d("Set attempt %s", attempt)
      pinEntryText?.setText(attempt)
      Timber.d("Set reentry %s", reentry)
      pinReentryText?.setText(reentry)
      Timber.d("Set hint %s", hint)
      pinHintText?.setText(hint)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    Timber.d("onSaveInstanceState")
    outState.putString(CODE_DISPLAY, getCurrentAttempt())
    outState.putString(CODE_REENTRY_DISPLAY, getCurrentReentry())
    outState.putString(HINT_DISPLAY, getCurrentHint())
    super.onSaveInstanceState(outState)
  }

  /**
   * Clear the display of all text entry fields
   */
  private fun clearDisplay() {
    pinEntryText?.setText("")
    pinReentryText?.setText("")
    pinHintText?.setText("")
  }

  @CheckResult private fun getCurrentAttempt(): String = pinEntryText?.text?.toString() ?: ""

  @CheckResult private fun getCurrentReentry(): String = pinReentryText?.text?.toString() ?: ""

  @CheckResult private fun getCurrentHint(): String = pinHintText?.text?.toString() ?: ""

  companion object {

    const internal val TAG = "PinEntryTextFragment"
    const private val CODE_DISPLAY = "CODE_DISPLAY"
    const private val CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY"
    const private val HINT_DISPLAY = "HINT_DISPLAY"
  }
}

