package com.pyamsoft.padlock.base.database

import androidx.annotation.CheckResult
import androidx.room.Database
import androidx.room.RoomDatabase
import com.pyamsoft.padlock.api.EntryDeleteDao
import com.pyamsoft.padlock.api.EntryInsertDao
import com.pyamsoft.padlock.api.EntryQueryDao
import com.pyamsoft.padlock.api.EntryUpdateDao
import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.EntityChangeEvent
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.DELETED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.INSERTED
import com.pyamsoft.padlock.model.db.EntityChangeEvent.Type.UPDATED
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

@Database(entities = [PadLockEntryDb::class], version = 1, exportSchema = true)
internal abstract class PadLockDbImpl internal constructor() : RoomDatabase(), PadLockDb {

  private val bus = RxBus.create<EntityChangeEvent>()

  private val queryDao = object : EntryQueryDao {

    override fun queryAll(): Single<List<AllEntriesModel>> {
      return queryDao().queryAll()
    }

    override fun subscribeToUpdates(): Observable<EntityChangeEvent> {
      return bus.listen()
    }

    override fun queryWithPackageName(packageName: String): Single<List<WithPackageNameModel>> {
      return queryDao().queryWithPackageName(packageName)
    }

    override fun queryWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Single<PadLockEntryModel> {
      return queryDao().queryWithPackageActivityName(packageName, activityName)
    }

  }

  private val insertDao = object : EntryInsertDao {

    override fun insert(entry: PadLockEntryModel): Completable {
      return insertDao().insert(entry)
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(
                    INSERTED,
                    entry.packageName(),
                    entry.activityName(),
                    entry.whitelist()
                )
            )
          }
    }

    override fun insert(
      packageName: String,
      activityName: String,
      lockCode: String?,
      lockUntilTime: Long,
      ignoreUntilTime: Long,
      isSystem: Boolean,
      whitelist: Boolean
    ): Completable {
      return insertDao().insert(
          packageName, activityName, lockCode, lockUntilTime, ignoreUntilTime, isSystem, whitelist
      )
          .doOnComplete {
            bus.publish(EntityChangeEvent(INSERTED, packageName, activityName, whitelist))
          }
    }

  }
  private val updateDao = object : EntryUpdateDao {
    override fun updateLockUntilTime(
      packageName: String,
      activityName: String,
      lockUntilTime: Long
    ): Completable {
      return updateDao().updateLockUntilTime(packageName, activityName, lockUntilTime)
    }

    override fun updateIgnoreUntilTime(
      packageName: String,
      activityName: String,
      ignoreUntilTime: Long
    ): Completable {
      return updateDao().updateIgnoreUntilTime(packageName, activityName, ignoreUntilTime)
    }

    override fun updateWhitelist(
      packageName: String,
      activityName: String,
      whitelist: Boolean
    ): Completable {
      return updateDao().updateWhitelist(packageName, activityName, whitelist)
          .doOnComplete {
            bus.publish(EntityChangeEvent(UPDATED, packageName, activityName, whitelist))
          }
    }

  }
  private val deleteDao = object : EntryDeleteDao {

    override fun deleteWithPackageName(packageName: String): Completable {
      return deleteDao().deleteWithPackageName(packageName)
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, packageName, null, false)
            )
          }
    }

    override fun deleteWithPackageActivityName(
      packageName: String,
      activityName: String
    ): Completable {
      return deleteDao().deleteWithPackageActivityName(packageName, activityName)
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, packageName, activityName, false)
            )
          }
    }

    override fun deleteAll(): Completable {
      return deleteDao().deleteAll()
          .doOnComplete {
            bus.publish(
                EntityChangeEvent(DELETED, null, null, false)
            )
          }
    }

  }

  init {
    Timber.i("Create new PadLockDbImpl")
  }

  override fun query(): EntryQueryDao {
    return queryDao
  }

  @CheckResult
  internal abstract fun queryDao(): DbEntryQueryDao

  override fun insert(): EntryInsertDao {
    return insertDao
  }

  @CheckResult
  internal abstract fun insertDao(): DbEntryInsertDao

  override fun update(): EntryUpdateDao {
    return updateDao
  }

  @CheckResult
  internal abstract fun updateDao(): DbEntryUpdateDao

  override fun delete(): EntryDeleteDao {
    return deleteDao
  }

  @CheckResult
  internal abstract fun deleteDao(): DbEntryDeleteDao

}

