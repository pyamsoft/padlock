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

package com.pyamsoft.padlock.service.device

import android.app.AppOpsManager
import android.app.AppOpsManager.OnOpChangedListener
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.getSystemService
import com.pyamsoft.padlock.api.preferences.PreferenceWatcher
import com.pyamsoft.padlock.api.service.UsageEventProvider
import com.pyamsoft.padlock.model.ForegroundEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.LazyThreadSafetyMode.NONE

@Singleton
internal class UsageEventProviderImpl @Inject internal constructor(
  private val context: Context
) : UsageEventProvider {

  private val usage by lazy(NONE) {
    requireNotNull(context.getSystemService<UsageStatsManager>())
  }

  override fun queryEvents(
    begin: Long,
    end: Long
  ): UsageEventProvider.EventQueryResult = EventQueryResultImpl(usage.queryEvents(begin, end))

  override fun watchPermission(func: (Boolean) -> Unit): PreferenceWatcher {
    return AppUsagePreferenceWatcher(context, func)
  }

  private class AppUsagePreferenceWatcher internal constructor(
    context: Context,
    func: (Boolean) -> Unit
  ) : PreferenceWatcher {

    private val appOps = requireNotNull(context.getSystemService<AppOpsManager>())
    private val callback = OnOpChangedListener { op, packageName ->
      if (op == AppOpsManager.OPSTR_GET_USAGE_STATS) {
        if (packageName == context.packageName) {
          func(UsagePermissionChecker.hasPermission(context))
        }
      }
    }

    init {
      // TODO: Causes memory leaks because stopWatchingMode leaks the callback
      // appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS, context.packageName, callback)
    }

    override fun stopWatching() {
      // TODO: Causes memory leaks because stopWatchingMode leaks the callback
      // Timber.d("Stop watching Usage Permission")
      // appOps.stopWatchingMode(callback)
    }

  }

  private data class EventQueryResultImpl(private val events: UsageEvents) : UsageEventProvider.EventQueryResult {

    private val event: UsageEvents.Event? by lazy {
      if (events.hasNextEvent()) {
        val event = UsageEvents.Event()
        events.getNextEvent(event)
        while (events.hasNextEvent()) {
          events.getNextEvent(event)
        }

        return@lazy event
      } else {
        return@lazy null
      }
    }

    override fun createForegroundEvent(func: (String, String) -> ForegroundEvent): ForegroundEvent {
      if ((event?.eventType ?: UsageEvents.Event.NONE) == UsageEvents.Event.MOVE_TO_FOREGROUND) {
        return func(event?.packageName.orEmpty(), event?.className.orEmpty())
      } else {
        return func("", "")
      }
    }
  }
}

