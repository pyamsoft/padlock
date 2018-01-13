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

package com.pyamsoft.padlock.list

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.pyamsoft.padlock.databinding.DialogInfoLocktypeExplainBinding
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.ui.helper.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled

class LockInfoExplanationDialog : CanaryDialog() {

    private lateinit var binding: DialogInfoLocktypeExplainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        binding = DialogInfoLocktypeExplainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lockInfoExplainToolbar.setUpEnabled(true)
            lockInfoExplainToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
                dismiss()
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }

    override fun onResume() {
        super.onResume()
        // The dialog is super small for some reason. We have to set the size manually, in onResume
        val window = dialog.window
        window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
    }

    companion object {
        const val TAG = "LockInfoExplainationDialog"
    }
}

