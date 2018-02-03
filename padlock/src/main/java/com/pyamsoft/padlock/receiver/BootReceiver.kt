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

package com.pyamsoft.padlock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pyamsoft.padlock.service.PadLockService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

  override fun onReceive(
      context: Context?,
      intent: Intent?
  ) {
    if (intent != null) {
      if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
        if (context != null) {
          Timber.d("Boot event received, start PadLockService")
          PadLockService.start(context)
        }
      }
    }
  }
}