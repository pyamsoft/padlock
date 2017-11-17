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
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding
import com.pyamsoft.padlock.list.ErrorDialog
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.helper.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.util.DialogUtil
import timber.log.Timber
import javax.inject.Inject

class LockScreenTextFragment : LockScreenBaseFragment(), LockEntryPresenter.View {

    private lateinit var imm: InputMethodManager
    private lateinit var binding: FragmentLockScreenTextBinding
    private var arrowGoTask = LoaderHelper.empty()
    private var editText: EditText? = null
    @Inject internal lateinit var presenter: LockEntryPresenter
    @Inject internal lateinit var imageLoader: ImageLoader

    override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

    @CheckResult private fun getCurrentAttempt(): String = editText?.text?.toString() ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.obtain<PadLockComponent>(context!!.applicationContext).plusLockScreenComponent(
                LockEntryModule(lockedPackageName, lockedActivityName, lockedRealName)).inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        binding = FragmentLockScreenTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        arrowGoTask = LoaderHelper.unload(arrowGoTask)
        activity?.let {
            imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
        }
        binding.unbind()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextInput()
        setupGoArrow()
        setupInputManager()
        clearDisplay()

        // Hide hint to begin with
        binding.lockDisplayHint.visibility = View.GONE

        presenter.bind(this)
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

    override fun onSaveInstanceState(outState: Bundle) {
        val attempt = getCurrentAttempt()
        outState.putString(CODE_DISPLAY, attempt)
        super.onSaveInstanceState(outState)
    }

    private fun setupInputManager() {
        // Force the keyboard
        imm = activity!!.applicationContext
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun setupGoArrow() {
        binding.lockImageGo.setOnDebouncedClickListener {
            submit()
            activity?.let {
                imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
            }
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
        arrowGoTask = imageLoader.fromResource(R.drawable.ic_arrow_forward_24dp)
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
        presenter.submit(lockedCode, getCurrentAttempt())
    }

    override fun onSubmitSuccess() {
        Timber.d("Unlocked!")
        clearDisplay()

        presenter.postUnlock(lockedCode, isLockedSystem, isExcluded, selectedIgnoreTime)
    }

    override fun onSubmitFailure() {
        Timber.e("Failed to unlock")
        clearDisplay()
        showSnackbarWithText("Error: Invalid PIN")
        binding.lockDisplayHint.visibility = View.VISIBLE

        // Display the hint if they fail unlocking
        presenter.displayLockedHint()

        // Once fail count is tripped once, continue to update it every time following until time elapses
        presenter.lockEntry()
    }

    override fun onSubmitError(throwable: Throwable) {
        clearDisplay()
        DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "unlock_error")
    }

    override fun onPostUnlocked() {
        presenter.passLockScreen()
        activity!!.finish()
    }

    override fun onUnlockError(throwable: Throwable) {
        clearDisplay()
        DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(),
                "lock_error")
    }

    override fun onLocked() {
        showSnackbarWithText("This entry is temporarily locked")
    }

    override fun onLockedError(throwable: Throwable) {
        clearDisplay()
        DialogUtil.guaranteeSingleDialogFragment(activity, ErrorDialog(), "lock_error")
    }

    override fun onDisplayHint(hint: String) {
        binding.lockDisplayHint.text = "Hint: %s".format(if (hint.isEmpty()) "NO HINT" else hint)
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
            fragment.arguments = LockScreenBaseFragment.buildBundle(lockedPackageName,
                    lockedActivityName,
                    lockedCode, lockedRealName, lockedSystem)
            return fragment
        }
    }
}
