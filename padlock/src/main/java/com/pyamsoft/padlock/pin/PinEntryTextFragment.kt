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
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentPinEntryTextBinding
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.ui.helper.Toasty
import com.pyamsoft.pydroid.ui.helper.setOnDebouncedClickListener
import timber.log.Timber
import javax.inject.Inject

class PinEntryTextFragment : PinEntryBaseFragment(), PinEntryPresenter.View {

    @field:Inject internal lateinit var presenter: PinEntryPresenter
    @field:Inject internal lateinit var imageLoader: ImageLoader
    private lateinit var imm: InputMethodManager
    private lateinit var binding: FragmentPinEntryTextBinding
    private var pinReentryText: EditText? = null
    private var pinEntryText: EditText? = null
    private var pinHintText: EditText? = null
    private var goTask = LoaderHelper.empty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        binding = FragmentPinEntryTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        goTask = LoaderHelper.unload(goTask)
        activity?.let {
            imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0, 0)
        }
        binding.unbind()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Resolve TextInputLayout edit texts
        pinEntryText = binding.pinEntryCode.editText!!
        pinReentryText = binding.pinReentryCode.editText!!
        pinHintText = binding.pinHint.editText!!

        // Force the keyboard
        imm = context!!.applicationContext
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)

        clearDisplay()
        setupGoArrow()

        binding.pinImageGo.setOnDebouncedClickListener {
            submitPin()
            activity?.let {
                imm.toggleSoftInputFromWindow(it.window.decorView.windowToken, 0,
                        0)
            }
        }

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        presenter.bind(viewLifecycle, this)
    }

    private fun setupSubmissionView(view: EditText) {
        view.setOnEditorActionListener { _, actionId, keyEvent ->
            if (keyEvent == null) {
                Timber.e("KeyEvent was not caused by keypress")
                return@setOnEditorActionListener false
            }

            if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
                Timber.d("KeyEvent is Enter pressed")
                submitPin()
                return@setOnEditorActionListener true
            }

            Timber.d("Do not handle key event")
            return@setOnEditorActionListener false
        }
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
        Toasty.makeText(context!!, throwable.message.toString(), Toasty.LENGTH_SHORT).show()
    }

    override fun onPinSubmitComplete() {
        clearDisplay()
        dismissParent()
    }

    private fun submitPin() {
        // Hint is blank for PIN code
        presenter.submit(getCurrentAttempt(), getCurrentReentry(), getCurrentHint())
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

    private fun setupGoArrow() {
        // Force keyboard focus
        pinEntryText?.requestFocus()

        goTask = LoaderHelper.unload(goTask)
        goTask = imageLoader.fromResource(R.drawable.ic_arrow_forward_24dp)
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

        internal const val TAG = "PinEntryTextFragment"
        private const val CODE_DISPLAY = "CODE_DISPLAY"
        private const val CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY"
        private const val HINT_DISPLAY = "HINT_DISPLAY"
    }
}

