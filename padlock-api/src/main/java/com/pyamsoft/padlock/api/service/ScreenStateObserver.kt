package com.pyamsoft.padlock.api.service

interface ScreenStateObserver {

  fun register(func: (Boolean) -> Unit)

  fun unregister()
}
