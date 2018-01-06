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

import com.pyamsoft.padlock.model.ClearPinEvent
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.RxBus
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton internal class ClearPinBus @Inject internal constructor() : EventBus<ClearPinEvent> {

    private val bus: EventBus<ClearPinEvent> = RxBus.create()

    override fun listen(): Observable<ClearPinEvent> = bus.listen()

    override fun publish(event: ClearPinEvent) {
        bus.publish(event)
    }
}
