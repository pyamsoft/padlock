package com.pyamsoft.padlock.lock

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.padlock.databinding.DialogLockStatBinding
import javax.inject.Inject
import javax.inject.Named

internal class LockStatViewImpl @Inject internal constructor(
  private val lifecycle: Lifecycle,
  private val inflater: LayoutInflater,
  @Named("locked_package_name") private val packageName: String,
  @Named("locked_activity_name") private val activityName: String,
  @Named("locked_real_name") private val realName: String,
  @Named("locked_system") private val isSystem: Boolean
) : LockStatView, LifecycleObserver {

  private lateinit var binding: DialogLockStatBinding

  init {
    lifecycle.addObserver(this)
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    lifecycle.removeObserver(this)

    binding.unbind()
  }

  override fun create() {
    binding = DialogLockStatBinding.inflate(inflater, null, false)

    binding.apply {
      statPackageName.text = packageName
      statRealName.text = realName
      statLockedBy.text = activityName
      statSystem.text = if (isSystem) "Yes" else "No"
    }
  }

  override fun setDisplayName(name: String) {
    binding.statDisplayName.text = name
  }

  override fun root(): View {
    return binding.root
  }
}
