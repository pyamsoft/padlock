/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.helper

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.util.tintWith

@CheckResult
fun MenuItem?.isChecked(): Boolean = this != null && isChecked

fun MenuItem?.setChecked(checked: Boolean) {
  if (this != null) {
    isChecked = checked
  }
}

fun Menu.tintIcon(
  context: Context,
  theming: Theming,
  @IdRes id: Int
) {
  val item = findItem(id)
  if (item != null) {
    val icon = item.icon
    if (icon != null) {
      item.icon = icon.mutate()
          .let {
            val color: Int
            if (theming.isDarkTheme()) {
              color = R.color.white
            } else {
              color = R.color.black
            }
            return@let it.tintWith(context, color)
          }
    }
  }
}
