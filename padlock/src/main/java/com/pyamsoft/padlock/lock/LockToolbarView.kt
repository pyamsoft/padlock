/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import timber.log.Timber
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

internal class LockToolbarView @Inject internal constructor(
  private val theming: Theming,
  private val toolbarActivityProvider: ToolbarActivityProvider,
  parent: ViewGroup
) : BaseUiView<Unit>(parent, Unit) {

  override val layout: Int = R.layout.light_toolbar

  private val toolbar by lazyView<Toolbar>(R.id.toolbar)
  private val menuIgnoreOne by lazyMenuItem(R.id.menu_ignore_one)
  private val menuIgnoreFive by lazyMenuItem(R.id.menu_ignore_five)
  private val menuIgnoreTen by lazyMenuItem(R.id.menu_ignore_ten)
  private val menuIgnoreFifteen by lazyMenuItem(R.id.menu_ignore_fifteen)
  private val menuIgnoreTwenty by lazyMenuItem(R.id.menu_ignore_twenty)
  private val menuIgnoreThirty by lazyMenuItem(R.id.menu_ignore_thirty)
  private val menuIgnoreFourtyFive by lazyMenuItem(R.id.menu_ignore_fourtyfive)
  private val menuIgnoreSixty by lazyMenuItem(R.id.menu_ignore_sixty)
  private val menuExclude by lazyMenuItem(R.id.menu_exclude)

  override fun id(): Int {
    return toolbar.id
  }

  @CheckResult
  fun isExcludeChecked(): Boolean {
    val result = menuExclude.isChecked
    Timber.d("Is Exclude checked: $result")
    return result
  }

  @CheckResult
  fun getSelectedIgnoreTime(): Long {
    val ignoreTimes = getIgnoreTimes()
    val result = when {
      menuIgnoreOne.isChecked -> ignoreTimes[0]
      menuIgnoreFive.isChecked -> ignoreTimes[1]
      menuIgnoreTen.isChecked -> ignoreTimes[2]
      menuIgnoreFifteen.isChecked -> ignoreTimes[3]
      menuIgnoreTwenty.isChecked -> ignoreTimes[4]
      menuIgnoreThirty.isChecked -> ignoreTimes[5]
      menuIgnoreFourtyFive.isChecked -> ignoreTimes[6]
      menuIgnoreSixty.isChecked -> ignoreTimes[7]
      else -> ignoreTimes[0]
    }

    Timber.d("Selected ignore time: $result")
    return result
  }

  @CheckResult
  private fun getIgnoreTimes(): List<Long> {
    val resources = toolbar.context.resources

    // Some extra list unwrapping and whatnot, but this should be proguarded out
    // and will help clean up logs
    val stringIgnoreTimes = arrayOf(*resources.getStringArray(R.array.ignore_time_entries))
    val ignoreTimes = ArrayList<Long>(stringIgnoreTimes.size)
    Timber.d("String ignore times: $stringIgnoreTimes")

    for (i in stringIgnoreTimes.indices) {
      ignoreTimes.add(stringIgnoreTimes[i].toLong())
    }

    Timber.d("Long ignore times: $ignoreTimes")
    return ignoreTimes
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onInflated(view, savedInstanceState)
    setupToolbar()
    inflateMenu(savedInstanceState)
    bindClicks()
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_PadLock_Dark_Lock
    } else {
      theme = R.style.ThemeOverlay_PadLock_Light_Lock
    }
    toolbar.popupTheme = theme
    ViewCompat.setElevation(toolbar, 0f)
    toolbarActivityProvider.setToolbar(toolbar)
  }

  private fun inflateMenu(savedInstanceState: Bundle?) {
    toolbar.inflateMenu(R.menu.lockscreen_menu)
    if (savedInstanceState != null) {
      restoreMenuState(savedInstanceState)
    }
  }

  private fun bindClicks() {
    // We must manually handle these onClicks because menu items suck.
    menuExclude.setOnMenuItemClickListener {
      it.isChecked = !it.isChecked
      return@setOnMenuItemClickListener true
    }

    val ignoreTimeMenuItems = getIgnoreTimeMenuItems()
    for (ignoreTimeMenuItem in ignoreTimeMenuItems) {
      ignoreTimeMenuItem.setOnMenuItemClickListener { clickedItem ->
        // If this click action causes a check event, we can fire
        if (!clickedItem.isChecked) {
          // Uncheck all the other item boxes
          ignoreTimeMenuItems
              .filterNot { it.itemId == clickedItem.itemId }
              .forEach { it.isChecked = false }

          // Check ourselves
          clickedItem.isChecked = true
        }
        return@setOnMenuItemClickListener true
      }
    }
  }

  override fun teardown() {
    super.teardown()
    menuExclude.setOnMenuItemClickListener(null)
    getIgnoreTimeMenuItems().forEach { it.setOnMenuItemClickListener(null) }
  }

  private fun restoreMenuState(state: Bundle) {
    menuExclude.isChecked = state.getBoolean(KEY_EXCLUDE, false)
    menuIgnoreOne.isChecked = state.getBoolean(KEY_IGNORE_TIME_ONE, false)
    menuIgnoreFive.isChecked = state.getBoolean(KEY_IGNORE_TIME_FIVE, false)
    menuIgnoreTen.isChecked = state.getBoolean(KEY_IGNORE_TIME_TEN, false)
    menuIgnoreFifteen.isChecked = state.getBoolean(KEY_IGNORE_TIME_FIFTEEN, false)
    menuIgnoreTwenty.isChecked = state.getBoolean(KEY_IGNORE_TIME_TWENTY, false)
    menuIgnoreThirty.isChecked = state.getBoolean(KEY_IGNORE_TIME_THIRTY, false)
    menuIgnoreFourtyFive.isChecked = state.getBoolean(KEY_IGNORE_TIME_FOURTYFIVE, false)
    menuIgnoreSixty.isChecked = state.getBoolean(KEY_IGNORE_TIME_SIXTY, false)
  }

  @CheckResult
  private fun getIgnoreTimeMenuItems(): List<MenuItem> {
    return listOf(
        menuIgnoreOne,
        menuIgnoreFive,
        menuIgnoreTen,
        menuIgnoreFifteen,
        menuIgnoreTwenty,
        menuIgnoreThirty,
        menuIgnoreFourtyFive,
        menuIgnoreSixty
    )
  }

  fun initIgnoreTime(time: Long) {
    val ignoreTimes = getIgnoreTimes()
    val menuItems = getIgnoreTimeMenuItems()

    for (index in ignoreTimes.indices) {
      val found = ignoreTimes[index]
      val checked = (found == time)
      if (checked) {
        Timber.d("Setting ignore time item checked: $time at $index")
        menuItems[index].isChecked = true
      }
    }
  }

  override fun saveState(outState: Bundle) {
    super.saveState(outState)
    outState.putBoolean(KEY_EXCLUDE, menuExclude.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_ONE, menuIgnoreOne.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_FIVE, menuIgnoreFive.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_TEN, menuIgnoreTen.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_FIFTEEN, menuIgnoreFifteen.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_TWENTY, menuIgnoreTwenty.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_THIRTY, menuIgnoreThirty.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_FOURTYFIVE, menuIgnoreFourtyFive.isChecked)
    outState.putBoolean(KEY_IGNORE_TIME_SIXTY, menuIgnoreSixty.isChecked)
  }

  @CheckResult
  private fun lazyMenuItem(@IdRes id: Int): Lazy<MenuItem> {
    return lazy(NONE) { toolbar.menu.findItem(id) }
  }

  fun setName(name: String) {
    toolbar.title = name
  }

  fun close() {
    Timber.d("A leak is reported, but this should dismiss the window, and clear the leak")
    toolbar.menu.close()
    toolbar.dismissPopupMenus()
  }

  companion object {

    private const val KEY_IGNORE_TIME_ONE = "KEY_IGNORE_TIME_ONE"
    private const val KEY_IGNORE_TIME_FIVE = "KEY_IGNORE_TIME_FIVE"
    private const val KEY_IGNORE_TIME_TEN = "KEY_IGNORE_TIME_TEN"
    private const val KEY_IGNORE_TIME_FIFTEEN = "KEY_IGNORE_TIME_FIFTEEN"
    private const val KEY_IGNORE_TIME_TWENTY = "KEY_IGNORE_TIME_TWENTY"
    private const val KEY_IGNORE_TIME_THIRTY = "KEY_IGNORE_TIME_THIRTY"
    private const val KEY_IGNORE_TIME_FOURTYFIVE = "KEY_IGNORE_TIME_FOURTYFIVE"
    private const val KEY_IGNORE_TIME_SIXTY = "KEY_IGNORE_TIME_SIXTY"
    private const val KEY_EXCLUDE = "KEY_EXCLUDE"
  }
}

