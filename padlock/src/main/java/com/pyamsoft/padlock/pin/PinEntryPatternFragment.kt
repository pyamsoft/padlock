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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.databinding.FragmentPinEntryPatternBinding
import com.pyamsoft.padlock.helper.LockCellUtil
import com.pyamsoft.pydroid.ui.helper.Toasty
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PinEntryPatternFragment : PinEntryBaseFragment(), PinEntryPresenter.View {

    @field:Inject internal lateinit var presenter: PinEntryPresenter
    private lateinit var binding: FragmentPinEntryPatternBinding
    private val cellPattern: MutableList<PatternLockView.Dot> = ArrayList()
    private val repeatCellPattern: MutableList<PatternLockView.Dot> = ArrayList()
    private var nextButtonOnClickRunnable: (() -> Boolean) = { false }
    private var patternText = ""
    private var listener: PatternLockViewListener? = null
    private var repeatPattern = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)

        if (savedInstanceState == null) {
            repeatPattern = false
            patternText = ""
        } else {
            repeatPattern = savedInstanceState.getBoolean(REPEAT_CELL_PATTERN, false)
            patternText = savedInstanceState.getString(PATTERN_TEXT, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                    binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG)
                    return@runnable false
                } else {
                    Timber.d("Submit initial attempt")
                    patternText = LockCellUtil.cellPatternToString(cellPattern)
                    repeatPattern = true
                    binding.patternLock.clearPattern()
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

    fun onNextButtonPressed() {
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
        Toasty.makeText(context!!, throwable.message.toString(), Toasty.LENGTH_SHORT).show()
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
        @JvmField internal var MINIMUM_PATTERN_LENGTH = 4
    }
}
