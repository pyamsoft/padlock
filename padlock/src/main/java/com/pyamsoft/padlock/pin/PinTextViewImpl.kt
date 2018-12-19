package com.pyamsoft.padlock.pin

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.databinding.FragmentPinEntryTextBinding
import com.pyamsoft.pydroid.ui.util.Snackbreak
import timber.log.Timber
import javax.inject.Inject

internal class PinTextViewImpl @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?
) : PinTextView, LifecycleObserver {

  private lateinit var binding: FragmentPinEntryTextBinding
  private lateinit var pinReentryText: EditText
  private lateinit var pinEntryText: EditText
  private lateinit var pinHintText: EditText

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    binding.unbind()
  }

  override fun clearDisplay() {
    pinEntryText.setText("")
    pinReentryText.setText("")
    pinHintText.setText("")
  }

  override fun onInvalidPin() {
    Snackbreak.short(binding.root, "Error incorrect PIN")
        .show()
  }

  override fun showReEntry(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit) {
    binding.pinReentryCode.visibility = View.VISIBLE
    binding.pinHint.visibility = View.VISIBLE
    setupSubmissionView(pinHintText, onSubmit)
  }

  override fun hideReEntry(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit) {
    binding.pinReentryCode.visibility = View.GONE
    binding.pinHint.visibility = View.GONE
    setupSubmissionView(pinEntryText, onSubmit)
  }

  override fun onSubmitPressed(onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit) {
    onSubmit(getAttempt(), getReEntry(), getHint())
  }

  override fun focus() {
    pinEntryText.requestFocus()
  }

  override fun create() {
    binding = FragmentPinEntryTextBinding.inflate(inflater, container, false)

    resolveEditTexts()
    restoreState(savedInstanceState)
  }

  override fun saveState(outState: Bundle) {
    outState.putString(CODE_DISPLAY, getAttempt())
    outState.putString(CODE_REENTRY_DISPLAY, getReEntry())
    outState.putString(HINT_DISPLAY, getHint())
  }

  override fun root(): View {
    return binding.root
  }

  private fun restoreState(state: Bundle?) {
    if (state == null) {
      return
    }

    val attempt = state.getString(CODE_DISPLAY, null)
    val reentry = state.getString(CODE_REENTRY_DISPLAY, null)
    val hint = state.getString(HINT_DISPLAY, null)

    if (attempt == null || reentry == null || hint == null) {
      clearDisplay()
    } else {
      pinEntryText.setText(attempt)
      pinReentryText.setText(reentry)
      pinHintText.setText(hint)
    }
  }

  private fun resolveEditTexts() {
    // Resolve TextInputLayout edit texts
    pinEntryText = requireNotNull(binding.pinEntryCode.editText)
    pinReentryText = requireNotNull(binding.pinReentryCode.editText)
    pinHintText = requireNotNull(binding.pinHint.editText)
  }

  @CheckResult
  private fun getAttempt(): String {
    return pinEntryText.text
        ?.toString()
        .orEmpty()
  }

  @CheckResult
  private fun getReEntry(): String {
    return pinReentryText.text
        ?.toString()
        .orEmpty()
  }

  @CheckResult
  private fun getHint(): String {
    return pinHintText.text
        ?.toString()
        .orEmpty()
  }

  private fun setupSubmissionView(
    view: EditText,
    onSubmit: (attempt: String, reEntry: String, hint: String) -> Unit
  ) {
    view.setOnEditorActionListener { _, actionId, keyEvent ->
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress")
        return@setOnEditorActionListener false
      }

      if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed")
        onSubmit(getAttempt(), getReEntry(), getHint())
        return@setOnEditorActionListener true
      }

      Timber.d("Do not handle key event")
      return@setOnEditorActionListener false
    }
  }

  companion object {

    private const val CODE_DISPLAY = "CODE_DISPLAY"
    private const val CODE_REENTRY_DISPLAY = "CODE_REENTRY_DISPLAY"
    private const val HINT_DISPLAY = "HINT_DISPLAY"
  }

}