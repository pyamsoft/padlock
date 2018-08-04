package com.pyamsoft.padlock.api.database

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import io.reactivex.Observable

interface EntityChangeDao {

  @CheckResult
  fun onChange(): Observable<EntityChangeEvent>
}
