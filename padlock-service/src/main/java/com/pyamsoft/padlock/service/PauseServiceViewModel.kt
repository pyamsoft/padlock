package com.pyamsoft.padlock.service

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.pin.CheckPinEvent
import com.pyamsoft.pydroid.core.bus.Listener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Named

class PauseServiceViewModel @Inject internal constructor(
  private val checkPinBus: Listener<CheckPinEvent>,
  @param:Named("recreate_listener") private val recreateListener: Listener<Unit>
) {

  @CheckResult
  fun onCheckPinEventSuccess(func: () -> Unit): Disposable {
    return checkPinBus.listen()
        .filter { it.matching }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onRecreateEvent(func: () -> Unit): Disposable {
    return recreateListener.listen()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

  @CheckResult
  fun onCheckPinEventFailed(func: () -> Unit): Disposable {
    return checkPinBus.listen()
        .filter { !it.matching }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { func() }
  }

}
