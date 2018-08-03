package com.pyamsoft.padlock.model.list

data class LockListUpdatePayload(
  val index: Int,
  val entry: AppEntry
)
