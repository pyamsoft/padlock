package com.pyamsoft.padlock.lock

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockToolbarView : BaseScreen {

  @CheckResult
  fun isExcludeChecked(): Boolean

  @CheckResult
  fun getSelectedIgnoreTime(): Long

}
