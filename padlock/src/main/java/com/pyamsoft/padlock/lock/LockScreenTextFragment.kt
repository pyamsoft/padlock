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
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding
import com.pyamsoft.padlock.list.ErrorDialog
import com.pyamsoft.padlock.lock.screen.LockScreenModule
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenTextFragment : LockScreenBaseFragment() {
  private lateinit var imm: InputMethodManager
  private lateinit var binding: FragmentLockScreenTextBinding
  private var arrowGoTask = LoaderHelper.empty()
  private var editText: EditText? = null
  @Inject internal lateinit var presenter: LockEntryPresenter

  @CheckResult private fun getCurrentAttempt(): String = editText?.text?.toString() ?: ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.with(context) {
      it.plusLockScreenComponent(LockScreenModule(lockedPackageName, lockedActivityName),
          LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName))
          .inject(this)
    }
  }

  override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
      savedInstanceState: Bundle?): View? {
    binding = FragmentLockScreenTextBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    arrowGoTask = LoaderHelper.unload(arrowGoTask)
    imm.toggleSoftInputFromWindow(activity.window.decorView.windowToken, 0, 0)
    binding.unbind()
  }

  override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupTextInput()
    setupGoArrow()
    setupInputManager()
    clearDisplay()

    // Hide hint to begin with
    binding.lockDisplayHint.visibility = View.GONE
  }

  override fun onStart() {
    super.onStart()
    presenter.start(Unit)
    presenter.displayLockedHint {
      Timber.d("Settings hint")
      binding.lockDisplayHint.text = "Hint: %s".format(if (it.isEmpty()) "NO HINT" else it)
    }
  }

  override fun onStop() {
    super.onStop()
    presenter.stop()
  }

  fun onRestoreInstanceState(savedInstanceState: Bundle) {
    val attempt = savedInstanceState.getString(CODE_DISPLAY, null)
    if (attempt == null) {
      Timber.d("Empty attempt")
      clearDisplay()
    } else {
      Timber.d("Set attempt %s", attempt)
      editText?.setText(attempt)
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    val attempt = getCurrentAttempt()
    outState?.putString(CODE_DISPLAY, attempt)
    super.onSaveInstanceState(outState)
  }

  private fun setupInputManager() {
    // Force the keyboard
    imm = activity.applicationContext
        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
  }

  private fun setupGoArrow() {
    binding.lockImageGo.setOnClickListener {
      submit()
      imm.toggleSoftInputFromWindow(activity.window.decorView.windowToken, 0,
          0)
    }

    // Force keyboard focus
    editText?.requestFocus()

    editText?.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY)
      }
    }

    arrowGoTask = LoaderHelper.unload(arrowGoTask)
    arrowGoTask = ImageLoader.fromResource(activity, R.drawable.ic_arrow_forward_24dp)
        .tint(R.color.orangeA200)
        .into(binding.lockImageGo)
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
          submit()
          return@setOnEditorActionListener true
        }

        Timber.d("Do not handle key event")
        return@setOnEditorActionListener false
      }
    }
  }

  private fun submit() {
    presenter.submit(lockedCode, getCurrentAttempt(), {
      Timber.d("Unlocked!")
      clearDisplay()

      presenter.postUnlock(lockedCode, isLockedSystem,
          isExcluded,
          selectedIgnoreTime, onPostUnlocked = {
        presenter.passLockScreen()
        activity.finish()
      }, onUnlockError = {
        clearDisplay()
        DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(),
            "lock_error")
      })
    }, onSubmitFailure = {
      Timber.e("Failed to unlock")
      clearDisplay()
      showSnackbarWithText("Error: Invalid PIN")
      binding.lockDisplayHint.visibility = View.VISIBLE

      // Once fail count is tripped once, continue to update it every time following until time elapses
      presenter.lockEntry(onLocked = {
        showSnackbarWithText("This entry is temporarily locked")
      }, onLockedError = {
        clearDisplay()
        DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "lock_error")
      })
    }, onSubmitError = {
      clearDisplay()
      DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "unlock_error")
    })
  }

  private fun clearDisplay() {
    editText?.setText("")
  }

  companion object {

    const internal val TAG = "LockScreenTextFragment"
    const private val CODE_DISPLAY = "CODE_DISPLAY"

    @JvmStatic
    @CheckResult
    fun newInstance(lockedPackageName: String,
        lockedActivityName: String, lockedCode: String?,
        lockedRealName: String, lockedSystem: Boolean): LockScreenTextFragment {
      val fragment = LockScreenTextFragment()
      fragment.arguments = LockScreenBaseFragment.buildBundle(lockedPackageName, lockedActivityName,
          lockedCode, lockedRealName, lockedSystem)
      return fragment
    }
  }
}