/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.padlock.list

import android.content.Context
import androidx.annotation.CheckResult
import com.popinnow.android.repo.MultiRepo
import com.popinnow.android.repo.Repo
import com.popinnow.android.repo.moshi.MoshiPersister
import com.popinnow.android.repo.newMultiRepo
import com.popinnow.android.repo.newRepoBuilder
import com.pyamsoft.padlock.model.list.ActivityEntry
import com.pyamsoft.padlock.model.list.AppEntry
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Named
import javax.inject.Singleton

@Module
object LockListSingletonProvider {

  @JvmStatic
  @Provides
  @Singleton
  @Named("repo_lock_list")
  internal fun provideLockListRepo(
    context: Context,
    moshi: Moshi
  ): Repo<List<AppEntry>> {
    val type = Types.newParameterizedType(List::class.java, AppEntry::class.java)
    return newRepoBuilder<List<AppEntry>>()
        .memoryCache(30, MINUTES)
        .persister(
            2, HOURS,
            File(context.cacheDir, "repo-lock-list"),
            MoshiPersister.create(moshi, type)
        )
        .build()
  }

  @JvmStatic
  @Provides
  @Singleton
  @Named("repo_lock_info")
  internal fun provideLockInfoRepo(
    context: Context,
    moshi: Moshi
  ): MultiRepo<List<ActivityEntry>> {
    val type = Types.newParameterizedType(List::class.java, ActivityEntry::class.java)
    return newMultiRepo {
      newRepoBuilder<List<ActivityEntry>>()
          .memoryCache(30, MINUTES)
          .persister(
              2, HOURS,
              File(context.cacheDir, "repo-$it"),
              MoshiPersister.create(addAdapterToMoshi(moshi), type)
          )
          .build()
    }
  }

  @CheckResult
  @JvmStatic
  private fun addAdapterToMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder()
        .add(ActivityEntryAdapter(moshi))
        .build()
  }

  private class ActivityEntryAdapter(moshi: Moshi) {

    private val itemAdapter = moshi.adapter(ActivityEntry.Item::class.java)
    private val groupAdapter = moshi.adapter(ActivityEntry.Group::class.java)

    @Suppress("unused")
    @CheckResult
    @ToJson
    fun toJson(entry: ActivityEntry): String {
      when (entry) {
        is ActivityEntry.Item -> {
          Timber.d("Convert $entry to Item json")
          return itemAdapter.toJson(entry)
        }
        is ActivityEntry.Group -> {
          Timber.d("Convert $entry to Group json")
          return groupAdapter.toJson(entry)
        }
      }
    }

    @Suppress("unused")
    @CheckResult
    @FromJson
    fun fromJson(json: String): ActivityEntry {
      // Only Item contains packageName field and lockState field
      // This is kinda flakey, but it works I guess
      val isItemType = json.contains("\"packageName\":") && json.contains("\"lockState\":")
      if (isItemType) {
        Timber.d("Convert $json to Item entry")
        return requireNotNull(itemAdapter.fromJson(json))
      } else {
        Timber.d("Convert $json to Group entry")
        return requireNotNull(groupAdapter.fromJson(json))
      }
    }
  }
}
