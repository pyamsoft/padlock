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
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqldelight.SqlDelightCompiledStatement
import com.squareup.sqldelight.SqlDelightCompiledStatement.Delete
import com.squareup.sqldelight.SqlDelightCompiledStatement.Insert
import com.squareup.sqldelight.SqlDelightCompiledStatement.Update
import com.squareup.sqldelight.SqlDelightStatement

@CheckResult
internal fun BriteDatabase.createQuery(query: SqlDelightStatement) = createQuery(query.tables,
        query.statement, *query.args)

@CheckResult
internal inline fun <T : SqlDelightCompiledStatement> BriteDatabase.bindAndExecute(
        statement: T, bind: T.() -> Unit): Long {
    synchronized(statement) {
        statement.bind()
        when (statement) {
            is Insert -> return executeInsert(statement.table, statement.program)
            is Update -> return executeUpdateDelete(statement.table, statement.program).toLong()
            is Delete -> return executeUpdateDelete(statement.table, statement.program).toLong()
            else -> throw IllegalStateException("Do not call execute() on query statements")
        }
    }
}