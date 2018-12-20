package com.pyamsoft.padlock.lock

import android.os.Bundle
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface LockScreenFragmentView : BaseScreen {

  fun clearDisplay()

  fun showSnackbar(text: String)

  fun saveState(outState: Bundle)

}
