package com.pyamsoft.padlock.pin

import com.pyamsoft.pydroid.ui.app.BaseScreen

interface PinBaseView : BaseScreen {

  fun clearDisplay()

  fun onPinCheckError()

  fun onInvalidPin()

  fun onPinSubmitError(error: Throwable)

}