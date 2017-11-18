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

package com.pyamsoft.padlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import timber.log.Timber

class ScreenEventListener(context: Context,
        private val eventListener: ForegroundEventListener) : BroadcastReceiver() {

    private var isRegistered: Boolean = false
    private val appContext: Context = context.applicationContext
    private val filter: IntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_SCREEN_ON)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (null != intent) {
            val action = intent.action
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Timber.d("Unregister foreground event listener on screen OFF")
                    eventListener.unregisterForegroundEventListener()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Timber.d("Register foreground event listener on screen ON")
                    eventListener.registerForegroundEventListener()
                }
                else -> Timber.e("Invalid event: %s", action)
            }
        }
    }

    fun register() {
        if (!isRegistered) {
            appContext.registerReceiver(this, filter)
            isRegistered = true
        } else {
            Timber.w("Already registered")
        }
    }

    fun unregister() {
        if (isRegistered) {
            appContext.unregisterReceiver(this)
            isRegistered = false
        } else {
            Timber.w("Already unregistered")
        }
    }
}
