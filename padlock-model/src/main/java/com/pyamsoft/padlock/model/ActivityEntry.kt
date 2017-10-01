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

package com.pyamsoft.padlock.model

import android.support.annotation.CheckResult
import com.google.auto.value.AutoValue

@AutoValue abstract class ActivityEntry protected constructor() {

  @CheckResult abstract fun toBuilder(): Builder

  @CheckResult abstract fun name(): String

  @CheckResult abstract fun packageName(): String

  @CheckResult abstract fun lockState(): LockState

  @CheckResult
  fun id(): String = "${packageName()}|${name()}"

  @AutoValue.Builder abstract class Builder {

    @CheckResult abstract fun name(s: String): Builder

    @CheckResult abstract fun packageName(s: String): Builder

    @CheckResult abstract fun lockState(state: LockState): Builder

    @CheckResult abstract fun build(): ActivityEntry
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun builder(): Builder = AutoValue_ActivityEntry.Builder()
  }
}
