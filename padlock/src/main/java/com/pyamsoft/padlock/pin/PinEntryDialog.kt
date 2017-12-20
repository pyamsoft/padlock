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
import android.support.annotation.CheckResult
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.pyamsoft.backstack.BackStack
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.loader.AppIconLoader
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.lock.screen.LockScreenInputPresenter
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.loader.LoaderHelper
import com.pyamsoft.pydroid.presenter.Presenter
import com.pyamsoft.pydroid.ui.helper.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.DrawableUtil
import timber.log.Timber
import javax.inject.Inject

class PinEntryDialog : CanaryDialog(), LockScreenInputPresenter.View {

    @field:Inject internal lateinit var presenter: LockScreenInputPresenter
    @field:Inject internal lateinit var appIconLoader: AppIconLoader
    private lateinit var binding: DialogPinEntryBinding
    private lateinit var packageName: String
    private var appIcon = LoaderHelper.empty()
    private lateinit var backstack: BackStack

    override fun provideBoundPresenters(): List<Presenter<*>> = listOf(presenter)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            packageName = it.getString(ENTRY_PACKAGE_NAME)
        }
        isCancelable = true

        Injector.obtain<PadLockComponent>(context!!.applicationContext).inject(this)
    }

    override fun onResume() {
        super.onResume()
        // The dialog is super small for some reason. We have to set the size manually, in onResume
        val window = dialog.window
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        backstack = BackStack.create(this, R.id.pin_entry_dialog_container)
        binding = DialogPinEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @CheckResult private fun pushIfNotPresent(pushFragment: PinEntryBaseFragment,
            tag: String): Boolean {
        val fragmentManager = childFragmentManager
        val fragment = fragmentManager.findFragmentByTag(tag)
        return if (fragment == null) {
            Timber.d("Push new pin fragment: $tag")
            backstack.set(tag) { pushFragment }
            // Return
            true
        } else {
            // Return
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        // Start hidden
        binding.pinNextButtonLayout.visibility = View.GONE

        presenter.bind(this)
    }

    override fun onStart() {
        super.onStart()
        appIcon = LoaderHelper.unload(appIcon)
        appIcon = appIconLoader.forPackageName(packageName).into(binding.pinImage)
    }

    override fun onTypePattern() {
        // Push text as child fragment
        Timber.d("Type Pattern")
        if (pushIfNotPresent(PinEntryPatternFragment(), PinEntryPatternFragment.TAG)) {
            binding.pinNextButtonLayout.visibility = View.VISIBLE
            binding.pinNextButton.setOnDebouncedClickListener {
                val fragmentManager = childFragmentManager
                val fragment = fragmentManager.findFragmentByTag(PinEntryPatternFragment.TAG)
                if (fragment is PinEntryPatternFragment) {
                    fragment.onNextButtonPressed()
                    binding.pinNextButton.text = "Submit"
                }
            }
        }
    }

    override fun onTypeText() {
        Timber.d("Type Text")
        if (pushIfNotPresent(PinEntryTextFragment(), PinEntryTextFragment.TAG)) {
            binding.pinNextButtonLayout.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        appIcon = LoaderHelper.unload(appIcon)
    }

    private fun setupToolbar() {
        // Maybe something more descriptive
        binding.apply {
            pinEntryToolbar.title = "PIN"
            pinEntryToolbar.setNavigationOnClickListener { dismiss() }
        }
        var icon = binding.pinEntryToolbar.navigationIcon
        if (icon != null) {
            icon = DrawableUtil.tintDrawableFromColor(icon,
                    ContextCompat.getColor(context!!, android.R.color.black))
            binding.pinEntryToolbar.navigationIcon = icon
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("Destroy AlertDialog")
        binding.unbind()
    }

    companion object {

        const val TAG = "PinEntryDialog"
        private const val ENTRY_PACKAGE_NAME = "entry_packagename"

        @JvmStatic
        @CheckResult
        fun newInstance(packageName: String): PinEntryDialog {
            return PinEntryDialog().apply {
                arguments = Bundle().apply {
                    putString(ENTRY_PACKAGE_NAME, packageName)
                }
            }
        }
    }
}
