package com.pyamsoft.padlock.api

import androidx.annotation.CheckResult
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single

interface EntryQueryDao {

  // Implemented in PadLockDbImpl using an EventBus that manually attaches to all modifications
  @CheckResult
  fun subscribeToUpdates(): Observable<EntityChangeEvent>

  @CheckResult
  fun queryAll(): Single<List<AllEntriesModel>>

  @CheckResult
  fun queryWithPackageName(packageName: String): Flowable<List<WithPackageNameModel>>

  @CheckResult
  fun queryWithPackageActivityName(
    packageName: String,
    activityName: String
  ): Single<PadLockEntryModel>

}
