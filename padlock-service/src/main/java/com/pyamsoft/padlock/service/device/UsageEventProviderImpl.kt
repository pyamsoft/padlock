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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.pyamsoft.padlock.api.UsageEventProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UsageEventProviderImpl @Inject internal constructor(context: Context) :
    UsageEventProvider {

  private val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

  override fun queryEvents(
      begin: Long,
      end: Long
  ): UsageEventProvider.EventQueryResult = EventQueryResultImpl(usage.queryEvents(begin, end))

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

    override fun hasMoveToForegroundEvent(): Boolean =
        (event?.eventType ?: UsageEvents.Event.NONE) == UsageEvents.Event.MOVE_TO_FOREGROUND

    override fun packageName(): String {
      if (!hasMoveToForegroundEvent()) {
        throw IllegalStateException("Result does not have an event, cannot get packageName")
      } else {
        return event?.packageName ?: ""
      }
    }

    override fun className(): String {
      if (!hasMoveToForegroundEvent()) {
        throw IllegalStateException("Result does not have an event, cannot get className")
      } else {
        return event?.className ?: ""
      }
    }

  }
}

