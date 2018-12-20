package com.pyamsoft.padlock.lock

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
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockScreenTextBinding
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import javax.inject.Inject

internal class LockScreenTextViewImpl @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?,
  private val imageLoader: ImageLoader
) : LockScreenTextView, LifecycleObserver {

  private lateinit var binding: FragmentLockScreenTextBinding
  private var editText: EditText? = null

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)
    binding.unbind()
  }

  override fun create() {
    binding = FragmentLockScreenTextBinding.inflate(inflater, container, false)

    setupTextInput()
    setupGoArrow()
    restoreState(savedInstanceState)
  }

  private fun restoreState(savedInstanceState: Bundle?) {
    val attempt = savedInstanceState?.getString(CODE_DISPLAY, null)
    if (attempt == null) {
      clearDisplay()
    } else {
      editText?.setText(attempt)
    }
  }

  override fun onGoClicked(onClick: (currentAttempt: String) -> Unit) {
    binding.lockImageGo.setOnDebouncedClickListener {
      onClick(getCurrentAttempt())
    }
  }

  override fun onFocusClaimed(onFocused: () -> Unit) {
    editText?.setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        onFocused()
      }
    }
  }

  override fun onEnterKeyPressed(onPress: (currentAttempt: String) -> Unit) {
    editText?.setOnEditorActionListener { _, actionId, keyEvent ->
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by key press")
        return@setOnEditorActionListener false
      } else {
        if (keyEvent.action == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
          Timber.d("KeyEvent is Enter pressed")
          onPress(getCurrentAttempt())
          return@setOnEditorActionListener true
        }

        Timber.d("Do not handle key event")
        return@setOnEditorActionListener false
      }
    }
  }

  private fun setupGoArrow() {
    // Force keyboard focus
    editText?.requestFocus()

    imageLoader.load(R.drawable.ic_check_24dp)
        .mutate {
          it.tintWith(root().context, R.color.white)
          return@mutate it
        }
        .into(binding.lockImageGo)
        .bind(owner)
  }

  private fun setupTextInput() {
    editText = binding.lockText.editText
  }

  override fun root(): View {
    return binding.root
  }

  override fun clearDisplay() {
    editText?.setText("")
    binding.lockDisplayHint.visibility = View.VISIBLE
  }

  override fun displayHint(hint: String) {
    binding.lockDisplayHint.text = "Hint: ${if (hint.isEmpty()) "NO HINT" else hint}"
  }

  override fun showSnackbar(text: String) {
    Snackbreak.short(root(), text)
        .show()
  }

  override fun saveState(outState: Bundle) {
    outState.putString(CODE_DISPLAY, getCurrentAttempt())
  }

  @CheckResult
  private fun getCurrentAttempt(): String = editText?.text?.toString() ?: ""

  companion object {
    private const val CODE_DISPLAY = "CODE_DISPLAY"
  }

}
