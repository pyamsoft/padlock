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

package com.pyamsoft.padlock.model;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue public abstract class AppEntry {

  @CheckResult @NonNull public static Builder builder() {
    return new AutoValue_AppEntry.Builder();
  }

  @CheckResult public abstract Builder toBuilder();

  @CheckResult public abstract String name();

  @CheckResult public abstract String packageName();

  @CheckResult public abstract boolean system();

  @CheckResult public abstract boolean locked();

  @CheckResult public abstract boolean otherLocked();

  @AutoValue.Builder public static abstract class Builder {

    @CheckResult public abstract Builder name(String s);

    @CheckResult public abstract Builder packageName(String s);

    @CheckResult public abstract Builder system(boolean b);

    @CheckResult public abstract Builder locked(boolean b);

    @CheckResult public abstract Builder otherLocked(boolean b);

    @CheckResult public abstract AppEntry build();
  }
}
