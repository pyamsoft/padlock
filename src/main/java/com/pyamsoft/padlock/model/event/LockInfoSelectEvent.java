/*
 * Copyright 2016 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.model.event;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;
import com.pyamsoft.padlock.model.LockState;

@AutoValue public abstract class LockInfoSelectEvent {

  @CheckResult @NonNull
  public static LockInfoSelectEvent create(int position, @NonNull String activityName,
      @NonNull LockState lockState) {
    return new AutoValue_LockInfoSelectEvent(position, activityName, lockState);
  }

  public abstract int position();

  public abstract String activityName();

  public abstract LockState lockState();
}
