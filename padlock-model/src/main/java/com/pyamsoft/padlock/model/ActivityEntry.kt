/*
 * Copyright 2017 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.padlock.model

import android.support.annotation.CheckResult
import com.google.auto.value.AutoValue

@AutoValue abstract class ActivityEntry protected constructor() {

  @CheckResult abstract fun toBuilder(): Builder

  @CheckResult abstract fun name(): String

  @CheckResult abstract fun packageName(): String

  @CheckResult abstract fun lockState(): LockState

  @CheckResult fun id(): String {
    return "${packageName()}|${name()}"
  }

  @AutoValue.Builder abstract class Builder {

    @CheckResult abstract fun name(s: String): Builder

    @CheckResult abstract fun packageName(s: String): Builder

    @CheckResult abstract fun lockState(state: LockState): Builder

    @CheckResult abstract fun build(): ActivityEntry
  }

  companion object {

    @JvmStatic
    @CheckResult fun builder(): Builder {
      return AutoValue_ActivityEntry.Builder()
    }
  }
}
