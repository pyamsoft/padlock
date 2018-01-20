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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.pyamsoft.padlock.Injector
import com.pyamsoft.padlock.PadLockComponent
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.base.AppIconLoader
import com.pyamsoft.padlock.databinding.DialogPinEntryBinding
import com.pyamsoft.padlock.lock.screen.LockScreenInputPresenter
import com.pyamsoft.padlock.uicommon.CanaryDialog
import com.pyamsoft.pydroid.util.DrawableUtil
import timber.log.Timber
import javax.inject.Inject

class PinEntryDialog : CanaryDialog(), LockScreenInputPresenter.View {

    @field:Inject
    internal lateinit var presenter: LockScreenInputPresenter
    @field:Inject
    internal lateinit var appIconLoader: AppIconLoader
    private lateinit var binding: DialogPinEntryBinding
    private lateinit var packageName: String

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
        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPinEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun pushIfNotPresent(pushFragment: PinEntryBaseFragment, tag: String) {
        val fragmentManager = childFragmentManager
        val fragment = fragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            Timber.d("Push new pin fragment: $tag")
            fragmentManager.beginTransaction().add(
                R.id.pin_entry_dialog_container, pushFragment,
                tag
            ).commit()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        presenter.bind(viewLifecycle, this)
    }

    override fun onStart() {
        super.onStart()
        appIconLoader.forPackageName(packageName).into(binding.pinImage).bind(viewLifecycle)
    }

    override fun onTypePattern() {
        // Push text as child fragment
        Timber.d("Type Pattern")
        pushIfNotPresent(PinEntryPatternFragment(), PinEntryPatternFragment.TAG)
    }

    override fun onTypeText() {
        Timber.d("Type Text")
        pushIfNotPresent(PinEntryTextFragment(), PinEntryTextFragment.TAG)
    }

    private fun setupToolbar() {
        // Maybe something more descriptive
        binding.apply {
            pinEntryToolbar.title = "PIN"
            pinEntryToolbar.setNavigationOnClickListener { dismiss() }

            // Set up icon as black
            var icon: Drawable? = pinEntryToolbar.navigationIcon
            if (icon != null) {
                icon = DrawableUtil.tintDrawableFromColor(
                    icon,
                    ContextCompat.getColor(pinEntryToolbar.context, android.R.color.black)
                )
                pinEntryToolbar.navigationIcon = icon
            }

            // Inflate menu
            pinEntryToolbar.inflateMenu(R.menu.pin_menu)

            // Make icon black
            val pinItem: MenuItem? = pinEntryToolbar.menu.findItem(R.id.menu_submit_pin)
            if (pinItem != null) {
                var pinIcon: Drawable? = pinItem.icon
                if (pinIcon != null) {
                    pinIcon = DrawableUtil.tintDrawableFromColor(
                        pinIcon,
                        ContextCompat.getColor(pinEntryToolbar.context, android.R.color.black)
                    )
                    pinItem.icon = pinIcon
                }
            }

            pinEntryToolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_submit_pin -> {
                        val fragmentManager = childFragmentManager
                        val fragment: Fragment? = fragmentManager.findFragmentById(
                            R.id.pin_entry_dialog_container
                        )
                        if (fragment is PinEntryBaseFragment) {
                            fragment.onSubmitPressed()
                            return@setOnMenuItemClickListener true
                        }
                    }
                }
                return@setOnMenuItemClickListener false
            }
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
