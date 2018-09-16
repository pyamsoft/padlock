package com.pyamsoft.padlock.service

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.padlock.model.pin.CheckPinEvent
import com.pyamsoft.pydroid.core.bus.Listener
import com.pyamsoft.pydroid.core.viewmodel.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class PauseServiceViewModel @Inject internal constructor(
  owner: LifecycleOwner,
  private val checkPinBus: Listener<CheckPinEvent>
) : BaseViewModel(owner) {

  fun onCheckPinEventSuccess(func: () -> Unit) {
    dispose {
      checkPinBus.listen()
          .filter { it.matching }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

  fun onCheckPinEventFailed(func: () -> Unit) {
    dispose {
      checkPinBus.listen()
          .filter { !it.matching }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { func() }
    }
  }

}
