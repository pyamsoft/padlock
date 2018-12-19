package com.pyamsoft.padlock.pin

import android.os.Bundle

interface PinTextView : PinBaseView {

  fun showReEntry(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit)

  fun hideReEntry(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit)

  fun onSubmitPressed(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit)

  fun focus()

  fun saveState(outState: Bundle)

}