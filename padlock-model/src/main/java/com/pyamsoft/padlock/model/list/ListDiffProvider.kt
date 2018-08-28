package com.pyamsoft.padlock.model.list

import androidx.annotation.CheckResult

interface ListDiffProvider<out T : Any> {

  @CheckResult
  fun data(): List<T>
}
