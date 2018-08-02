package com.pyamsoft.padlock.model.db

data class EntityChangeEvent(
  val type: Type,
  val packageName: String?,
  val activityName: String?,
  val whitelisted: Boolean
) {

  enum class Type {
    DELETED,
    UPDATED,
    INSERTED
  }
}
