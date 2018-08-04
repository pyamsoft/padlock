package com.pyamsoft.padlock.base.database

import android.content.Context
import androidx.annotation.CheckResult
import androidx.room.Room
import com.pyamsoft.padlock.api.EntryDeleteDao
import com.pyamsoft.padlock.api.EntryInsertDao
import com.pyamsoft.padlock.api.EntryQueryDao
import com.pyamsoft.padlock.api.EntryUpdateDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseProvider {

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDb(context: Context): PadLockDb {
    return Room.databaseBuilder(
        context.applicationContext,
        PadLockDbImpl::class.java,
        PadLockDb.DATABASE_NAME
    )
        .build()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideQueryDao(db: PadLockDb): EntryQueryDao {
    return db.query()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideInsertDao(db: PadLockDb): EntryInsertDao {
    return db.insert()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDeleteDao(db: PadLockDb): EntryDeleteDao {
    return db.delete()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideUpdateDao(db: PadLockDb): EntryUpdateDao {
    return db.update()
  }

}
