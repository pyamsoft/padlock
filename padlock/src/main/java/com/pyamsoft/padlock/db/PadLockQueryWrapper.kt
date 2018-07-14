package com.pyamsoft.padlock.db

import android.content.Context
import androidx.annotation.CheckResult
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.pyamsoft.padlock.QueryWrapper

internal object PadLockQueryWrapper {

  private const val DATABASE_VERSION = 1
  private const val DATABASE_NAME = "com.pyamsoft.padlock.PadLockEntrySql.db"
  private const val OLD_DATABASE_NAME = "padlock_db"

  @CheckResult
  @JvmStatic
  fun create(context: Context): QueryWrapper {
    val config = SupportSQLiteOpenHelper.Configuration.builder(context.applicationContext)
        .name(DATABASE_NAME)
        .callback(Callback(context.applicationContext))
        .build()
    val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
    // TODO QueryWrapper(SqlDelightDatabaseHelper(openHelper))
    TODO("QueryWrapper AndroidX")
  }

  internal class Callback internal constructor(
    context: Context
  ) : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    override fun onCreate(db: SupportSQLiteDatabase) {
      // TODO val database = SqlDelight.create(QueryWrapper.Companion, db)
      // TODO QueryWrapper.onCreate(database)

      // TODO Manually migrate from Old DB version to new stuff
    }

    override fun onUpgrade(
      db: SupportSQLiteDatabase,
      oldVersion: Int,
      newVersion: Int
    ) {
      // TODO QueryWrapper.onMigrate(db, oldVersion, newVersion)
    }
  }
}
