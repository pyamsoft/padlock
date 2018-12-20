package com.pyamsoft.padlock.lock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

internal class LockScreenPatternViewImpl @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?
) : LockScreenPatternView, LifecycleObserver {

  private lateinit var binding: FragmentLockScreenPatternBinding
  private var listener: PatternLockViewListener? = null

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)
    clearPatternListener()
    binding.unbind()
  }

  override fun create() {
    binding = FragmentLockScreenPatternBinding.inflate(inflater, container, false)
    setupPatternView()
  }

  override fun saveState(outState: Bundle) {
  }

  private fun setupPatternView() {

    // Dots always white
    binding.patternLock.isTactileFeedbackEnabled = false
    binding.patternLock.normalStateColor = ContextCompat.getColor(root().context, R.color.white)
  }

  private fun clearPatternListener() {
    listener?.let { binding.patternLock.removePatternLockListener(it) }
    listener = null
  }

  override fun onPatternComplete(onComplete: (list: List<Dot>) -> Unit) {
    clearPatternListener()

    listener = object : PatternLockViewListener {
      override fun onStarted() {
      }

      override fun onProgress(list: List<PatternLockView.Dot>) {
      }

      override fun onComplete(list: List<PatternLockView.Dot>) {
        onComplete(list)
      }

      override fun onCleared() {
      }
    }

    binding.patternLock.addPatternLockListener(listener)
  }

  override fun root(): View {
    return binding.root
  }

  override fun clearDisplay() {
    binding.patternLock.clearPattern()
  }

  override fun showSnackbar(text: String) {
    Snackbreak.short(root(), text)
        .show()
  }

}