package com.pyamsoft.padlock.settings

import com.pyamsoft.pydroid.ui.app.BaseView

interface SettingsView : BaseView {

  fun onLockTypeChangeAttempt(onChange: (newValue: String) -> Unit)

  fun changeLockType(newValue: String)

  fun onInstallListenerClicked(onClick: () -> Unit)

  fun onClearDatabaseClicked(onClick: () -> Unit)

}