package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.ActivityLockBinding
import com.pyamsoft.padlock.helper.isChecked
import com.pyamsoft.padlock.helper.setChecked
import com.pyamsoft.padlock.loader.AppIconLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@LockScreen
internal class LockScreenViewImpl @Inject internal constructor(
  @Named("locked_package_name") private val lockedPackageName: String,
  @Named("activity_bundle") private val savedInstanceState: Bundle?,
  @Named("locked_icon") private val lockedIcon: Int,
  private val activity: LockScreenActivity,
  private val appIconLoader: AppIconLoader,
  private val theming: Theming
) : LockScreenView, LockToolbarView {

  private lateinit var binding: ActivityLockBinding

  private var ignoreIndex: Int = 0
  private var excludeEntry: Boolean = false

  // These can potentially be unassigned in onSaveInstanceState, mark them nullable
  private var menuIgnoreOne: MenuItem? = null
  private var menuIgnoreFive: MenuItem? = null
  private var menuIgnoreTen: MenuItem? = null
  private var menuIgnoreFifteen: MenuItem? = null
  private var menuIgnoreTwenty: MenuItem? = null
  private var menuIgnoreThirty: MenuItem? = null
  private var menuIgnoreFourtyFive: MenuItem? = null
  private var menuIgnoreSixty: MenuItem? = null
  private var menuExclude: MenuItem? = null

  private lateinit var ignoreTimes: MutableList<Long>

  override fun isExcludeChecked(): Boolean {
    return menuExclude.isChecked()
  }

  override fun getSelectedIgnoreTime(): Long {
    val index = getSelectedIgnoreIndex()
    return ignoreTimes[index]
  }

  @CheckResult
  private fun getSelectedIgnoreIndex(): Int {
    var index: Int
    try {
      index = when {
        menuIgnoreOne.isChecked() -> 0
        menuIgnoreFive.isChecked() -> 1
        menuIgnoreTen.isChecked() -> 2
        menuIgnoreFifteen.isChecked() -> 3
        menuIgnoreTwenty.isChecked() -> 4
        menuIgnoreThirty.isChecked() -> 5
        menuIgnoreFourtyFive.isChecked() -> 6
        menuIgnoreSixty.isChecked() -> 7
        else -> 0
      }
    } catch (e: NullPointerException) {
      Timber.w("NULL menu item, default to first option")
      index = 0
    }

    return index
  }

  override fun create() {
    binding = DataBindingUtil.setContentView(activity, R.layout.activity_lock)

    populateIgnoreTimes()
    restoreState(savedInstanceState)
    loadAppIcon()
    setupToolbar()
  }

  private fun populateIgnoreTimes() {
    val stringIgnoreTimes = activity.resources.getStringArray(R.array.ignore_time_entries)
    ignoreTimes = ArrayList(stringIgnoreTimes.size)
    for (i in stringIgnoreTimes.indices) {
      ignoreTimes.add(stringIgnoreTimes[i].toLong())
    }
  }

  private fun restoreState(savedInstanceState: Bundle?) {
    ignoreIndex = savedInstanceState?.getInt(KEY_IGNORE_TIME, NO_IGNORE_TIME_PICKED)
        ?: NO_IGNORE_TIME_PICKED
    excludeEntry = savedInstanceState?.getBoolean(KEY_EXCLUDE, false)
        ?: false
  }

  override fun saveState(outState: Bundle) {
    outState.apply {
      putInt(KEY_IGNORE_TIME, getSelectedIgnoreIndex())
      putBoolean(KEY_EXCLUDE, isExcludeChecked())
    }
  }

  private fun loadAppIcon() {
    appIconLoader.loadAppIcon(lockedPackageName, lockedIcon)
        .into(binding.lockImage)
        .bind(activity)
  }

  private fun setupToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.ThemeOverlay_PadLock_Dark_Lock
    } else {
      theme = R.style.ThemeOverlay_PadLock_Light_Lock
    }
    binding.toolbar.apply {
      popupTheme = theme

      activity.setToolbar(this)
      ViewCompat.setElevation(this, 0f)

      inflateMenu(R.menu.lockscreen_menu)
      menu.let {
        menuIgnoreOne = it.findItem(R.id.menu_ignore_one)
        menuIgnoreFive = it.findItem(R.id.menu_ignore_five)
        menuIgnoreTen = it.findItem(R.id.menu_ignore_ten)
        menuIgnoreFifteen = it.findItem(R.id.menu_ignore_fifteen)
        menuIgnoreTwenty = it.findItem(R.id.menu_ignore_twenty)
        menuIgnoreThirty = it.findItem(R.id.menu_ignore_thirty)
        menuIgnoreFourtyFive = it.findItem(R.id.menu_ignore_fourtyfive)
        menuIgnoreSixty = it.findItem(R.id.menu_ignore_sixty)
        menuExclude = it.findItem(R.id.menu_exclude)
      }
    }

    menuExclude.setChecked(excludeEntry)
  }

  override fun closeToolbar() {
    Timber.d("A leak is reported, but this should dismiss the window, and clear the leak")
    binding.toolbar.menu.close()
    binding.toolbar.dismissPopupMenus()
  }

  override fun onToolbarNavigationClicked(onClick: () -> Unit) {
    binding.toolbar.setNavigationOnClickListener(DebouncedOnClickListener.create { onClick() })
  }

  override fun onMenuItemClicked(onClick: (item: MenuItem) -> Unit) {
    binding.toolbar.setOnMenuItemClickListener {
      onClick(it)
      return@setOnMenuItemClickListener true
    }
  }

  override fun setToolbarTitle(title: String) {
    binding.toolbar.title = title
  }

  override fun root(): View {
    // Do not expose full root, just container
    return binding.lockScreenContainer
  }

  override fun initIgnoreTimeSelection(time: Long) {
    var index = -1
    for (i in ignoreTimes.indices) {
      if (ignoreTimes[i] == time) {
        index = i
        break
      }
    }

    // Set index to the first option if it is unset
    if (index < 0) {
      index = 0
    }

    val apply: Int
    if (ignoreIndex == NO_IGNORE_TIME_PICKED) {
      // Apply defaults from prefs
      apply = index
    } else {
      // Restore state
      apply = ignoreIndex
    }

    when (apply) {
      1 -> menuIgnoreFive.setChecked(true)
      2 -> menuIgnoreTen.setChecked(true)
      3 -> menuIgnoreFifteen.setChecked(true)
      4 -> menuIgnoreTwenty.setChecked(true)
      5 -> menuIgnoreThirty.setChecked(true)
      6 -> menuIgnoreFourtyFive.setChecked(true)
      7 -> menuIgnoreSixty.setChecked(true)
      else -> menuIgnoreOne.setChecked(true)
    }
  }

  companion object {

    private const val NO_IGNORE_TIME_PICKED = -1
    private const val KEY_IGNORE_TIME = "key_ignore_time"
    private const val KEY_EXCLUDE = "key_exclude"
  }
}
