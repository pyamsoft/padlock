package com.pyamsoft.padlock.lock

interface LockScreenTextView : LockScreenFragmentView {

  fun displayHint(hint: String)

  fun onGoClicked(onClick: (currentAttempt: String) -> Unit)

  fun onEnterKeyPressed(onPress: (currentAttempt: String) -> Unit)

  fun onFocusClaimed(onFocused: () -> Unit)

}
