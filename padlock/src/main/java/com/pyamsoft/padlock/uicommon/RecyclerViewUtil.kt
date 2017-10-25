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

package com.pyamsoft.padlock.uicommon

import android.support.annotation.CheckResult
import android.support.v7.widget.RecyclerView.ItemAnimator
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator

object RecyclerViewUtil {

  @CheckResult
  fun withStandardDurations(animator: ItemAnimator = SlideInUpAnimator()): ItemAnimator {
    return animator.apply {
      addDuration = 300L
      moveDuration = 300L
      removeDuration = 400L
      changeDuration = 10L
    }
  }
}
