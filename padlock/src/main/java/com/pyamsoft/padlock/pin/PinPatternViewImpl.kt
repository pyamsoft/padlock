package com.pyamsoft.padlock.pin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.PatternLockView.Dot
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockScreenPatternBinding
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

internal class PinPatternViewImpl @Inject internal constructor(
  private val theming: Theming,
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?
) : PinPatternView, LifecycleObserver {

  private lateinit var binding: FragmentLockScreenPatternBinding
  private var listener: PatternLockViewListener? = null

  init {
    owner.lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    listener?.let { binding.patternLock.removePatternLockListener(it) }
    listener = null

    binding.unbind()
  }

  override fun root(): View {
    return binding.root
  }

  override fun create() {
    binding = FragmentLockScreenPatternBinding.inflate(inflater, container, false)

    setupLockView()
  }

  private fun setupLockView() {
    val theme: Int
    if (theming.isDarkTheme()) {
      theme = R.style.Theme_PadLock_Dark_Dialog
    } else {
      theme = R.style.Theme_PadLock_Light_Dialog
    }

    root().context.withStyledAttributes(
        theme,
        intArrayOf(android.R.attr.colorForeground)
    ) {
      val colorId = getResourceId(0, 0)
      if (colorId != 0) {
        binding.patternLock.normalStateColor = ContextCompat.getColor(root().context, colorId)
      }
    }
  }

  override fun onPatternEntry(
    onPattern: (list: List<Dot>) -> Unit,
    onClear: () -> Unit
  ) {
    listener = object : PatternLockViewListener {

      override fun onStarted() {
        setPatternCorrect()
        onClear()
      }

      override fun onProgress(list: List<PatternLockView.Dot>) {
      }

      override fun onComplete(list: List<PatternLockView.Dot>) {
        onPattern(list)
      }

      override fun onCleared() {
        onClear()
      }
    }

    binding.apply {
      patternLock.isTactileFeedbackEnabled = false
      patternLock.addPatternLockListener(listener)
    }
  }

  override fun clearDisplay() {
    binding.patternLock.clearPattern()
  }

  override fun setPatternCorrect() {
    binding.patternLock.setViewMode(PatternLockView.PatternViewMode.CORRECT)
  }

  override fun setPatternWrong() {
    binding.patternLock.setViewMode(PatternLockView.PatternViewMode.WRONG)
  }

  override fun infoPatternNeedsRepeat() {
    clearDisplay()
    Snackbreak.short(binding.root, "Please confirm pattern")
        .show()
  }

  override fun infoPatternNotLongEnough() {
    Snackbreak.short(binding.root, "Pattern is not long enough")
        .show()
    setPatternWrong()
  }

  override fun onInvalidPin() {
    Snackbreak.short(binding.root, "Error incorrect PIN")
        .show()
  }

}