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
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class LockScreenTextFragment : LockScreenBaseFragment() {

  private val imm by lazy(NONE) {
    requireNotNull(requireActivity().application.getSystemService<InputMethodManager>())
  }

  private lateinit var binding: FragmentLockScreenTextBinding
  private var editText: EditText? = null

  @CheckResult
  private fun getCurrentAttempt(): String = editText?.text?.toString() ?: ""

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = FragmentLockScreenTextBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupTextInput()
    setupGoArrow()
    setupInputManager()
    clearDisplay()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activity?.let {
      imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
    }
    binding.unbind()
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    val attempt = savedInstanceState.getString(CODE_DISPLAY, null)
    if (attempt == null) {
      Timber.d("Empty attempt")
      clearDisplay()
    } else {
      Timber.d("Set attempt %s", attempt)
      editText?.setText(attempt)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    val attempt = getCurrentAttempt()
    outState.putString(CODE_DISPLAY, attempt)
    super.onSaveInstanceState(outState)
  }

  private fun setupInputManager() {
    // Force the keyboard
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
  }

  private fun setupGoArrow() {
    binding.lockImageGo.setOnDebouncedClickListener { _ ->
      submitPin(getCurrentAttempt())
      activity?.also {
        imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
      }
    }

    // Force keyboard focus
    editText?.requestFocus()

    editText?.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
      }
    }

    imageLoader.load(R.drawable.ic_check_24dp)
        .mutate {
          it.tintWith(requireActivity(), R.color.white)
          return@mutate it
        }
        .into(binding.lockImageGo)
        .bind(viewLifecycleOwner)
  }

  private fun setupTextInput() {
    editText = binding.lockText.editText
    editText?.setOnEditorActionListener { _, actionId, keyEvent ->
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by key press")
        return@setOnEditorActionListener false
      } else {
        if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
          Timber.d("KeyEvent is Enter pressed")
          submitPin(getCurrentAttempt())
          return@setOnEditorActionListener true
        }

        Timber.d("Do not handle key event")
        return@setOnEditorActionListener false
      }
    }
  }

  override fun onDisplayHint(hint: String) {
    binding.lockDisplayHint.text = "Hint: ${if (hint.isEmpty()) "NO HINT" else hint}"
  }

  override fun clearDisplay() {
    editText?.setText("")
    binding.lockDisplayHint.visibility = View.VISIBLE
  }

  companion object {

    internal const val TAG = "LockScreenTextFragment"
    private const val CODE_DISPLAY = "CODE_DISPLAY"

    @JvmStatic
    @CheckResult
    fun newInstance(
      lockedPackageName: String,
      lockedActivityName: String,
      lockedCode: String?,
      lockedRealName: String,
      lockedSystem: Boolean
    ): LockScreenTextFragment {
      val fragment = LockScreenTextFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(
          lockedPackageName,
          lockedActivityName,
          lockedCode, lockedRealName, lockedSystem
      )
      return fragment
    }
  }
}
