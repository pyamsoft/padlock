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

package com.pyamsoft.padlock.model

import com.pyamsoft.padlock.model.db.AllEntriesModel
import com.pyamsoft.padlock.model.db.PadLockEntryModel
import com.pyamsoft.padlock.model.db.WithPackageNameModel

internal data class PadLockEntry internal constructor(
  private val packageName: String,
  private val activityName: String,
  private val lockCode: String?,
  private val whitelist: Boolean,
  private val systemApplication: Boolean,
  private val ignoreUntilTime: Long,
  private val lockUntilTime: Long
) : PadLockEntryModel {

  override fun packageName(): String {
    return packageName
  }

  override fun activityName(): String {
    return activityName
  }

  override fun lockCode(): String? {
    return lockCode
  }

  override fun whitelist(): Boolean {
    return whitelist
  }

  override fun systemApplication(): Boolean {
    return systemApplication
  }

  override fun ignoreUntilTime(): Long {
    return ignoreUntilTime
  }

  override fun lockUntilTime(): Long {
    return lockUntilTime
  }

  data class AllEntries internal constructor(
    private val packageName: String,
    private val activityName: String,
    private val whitelist: Boolean
  ) : AllEntriesModel {

    override fun packageName(): String {
      return packageName
    }

    override fun activityName(): String {
      return activityName
    }

    override fun whitelist(): Boolean {
      return whitelist
    }

  }

  data class A internal constructor(
    private val activityName: String,
    private val whitelist: Boolean
  ) : WithPackageNameModel {

    override fun activityName(): String {
      return activityName
    }

    override fun whitelist(): Boolean {
      return whitelist
    }

  }
}
