package com.pyamsoft.padlock.lock

import com.andrognito.patternlockview.PatternLockView.Dot

interface LockScreenPatternView : LockScreenFragmentView {

  fun onPatternComplete(onComplete: (list: List<Dot>) -> Unit)

}
