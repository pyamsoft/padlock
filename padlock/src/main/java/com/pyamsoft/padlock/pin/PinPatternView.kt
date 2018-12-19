package com.pyamsoft.padlock.pin

import com.andrognito.patternlockview.PatternLockView
import com.pyamsoft.pydroid.ui.app.BaseScreen

interface PinPatternView : BaseScreen {

  fun onPatternEntry(
    onPattern: (list: List<PatternLockView.Dot>) -> Unit,
    onClear: () -> Unit
  )

  fun setPatternCorrect()

  fun setPatternWrong()

  fun clearDisplay()

  fun infoPatternNotLongEnough()

  fun infoPatternNeedsRepeat()

  fun onPinCheckError()

  fun onInvalidPin()

  fun onPinSubmitError(error: Throwable)

}