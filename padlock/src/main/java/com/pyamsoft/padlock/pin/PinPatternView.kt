package com.pyamsoft.padlock.pin

import com.andrognito.patternlockview.PatternLockView

interface PinPatternView : PinBaseView {

  fun onPatternEntry(
    onPattern: (list: List<PatternLockView.Dot>) -> Unit,
    onClear: () -> Unit
  )

  fun setPatternCorrect()

  fun setPatternWrong()

  fun infoPatternNotLongEnough()

  fun infoPatternNeedsRepeat()

}