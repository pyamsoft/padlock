/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.base.db

import android.support.annotation.CheckResult
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqldelight.SqlDelightCompiledStatement
import com.squareup.sqldelight.SqlDelightCompiledStatement.*
import com.squareup.sqldelight.SqlDelightStatement

@CheckResult
internal fun BriteDatabase.createQuery(query: SqlDelightStatement) = createQuery(
    query.tables,
    query.statement, *query.args
)

@CheckResult
internal inline fun <T : SqlDelightCompiledStatement> BriteDatabase.bindAndExecute(
    statement: T,
    bind: T.() -> Unit
): Long {
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