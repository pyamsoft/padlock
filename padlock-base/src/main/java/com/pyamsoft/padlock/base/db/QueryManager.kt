/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.base.db

import android.support.annotation.CheckResult
import com.pyamsoft.padlock.model.PadLockEntry
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel.WithPackageNameModel
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqldelight.RowMapper
import io.reactivex.Single
import java.util.Collections

internal class QueryManager internal constructor(private val briteDatabase: BriteDatabase) {

    private val withPackageActivityNameDefaultMapper: RowMapper<out PadLockEntryModel> by lazy {
        PadLockSqlEntry.FACTORY.withPackageActivityNameDefaultMapper()
    }

    private val allEntriesMapper: RowMapper<out AllEntriesModel> by lazy {
        PadLockSqlEntry.FACTORY.allEntriesMapper { packageName, activityName, whitelist ->
            AutoValue_PadLockSqlEntry_AllEntries(packageName, activityName, whitelist)
        }
    }

    private val withPackageNameMapper: RowMapper<out WithPackageNameModel> by lazy {
        PadLockSqlEntry.FACTORY.withPackageNameMapper { activityName, whitelist ->
            AutoValue_PadLockSqlEntry_WithPackageName(activityName, whitelist)
        }
    }

    @CheckResult internal fun queryWithPackageActivityNameDefault(packageName: String,
            activityName: String): Single<PadLockEntryModel> {
        val statement = PadLockSqlEntry.FACTORY.withPackageActivityNameDefault(packageName,
                PadLockEntry.PACKAGE_ACTIVITY_NAME, activityName)
        return briteDatabase.createQuery(statement)
                .mapToOne { withPackageActivityNameDefaultMapper.map(it) }
                .first(PadLockEntry.EMPTY)
    }

    @CheckResult internal fun queryWithPackageName(
            packageName: String): Single<List<WithPackageNameModel>> {
        val statement = PadLockSqlEntry.FACTORY.withPackageName(packageName)
        return briteDatabase.createQuery(statement)
                .mapToList { withPackageNameMapper.map(it) }
                .first(emptyList())
                .map { Collections.unmodifiableList(it) }
    }

    @CheckResult internal fun queryAll(): Single<List<AllEntriesModel>> {
        val statement = PadLockSqlEntry.FACTORY.allEntries()
        return briteDatabase.createQuery(statement)
                .mapToList { allEntriesMapper.map(it) }
                .first(emptyList())
                .map { Collections.unmodifiableList(it) }
    }
}

