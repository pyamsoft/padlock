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

import com.google.auto.value.AutoValue;

@AutoValue public abstract class AppEntry {

  public static Builder builder() {
    return new AutoValue_AppEntry.Builder();
  }

  public static Builder builder(final AppEntry entry) {
    return new AutoValue_AppEntry.Builder(entry);
  }

  public abstract String name();

  public abstract String packageName();

  public abstract boolean system();

  public abstract boolean locked();

  @AutoValue.Builder public static abstract class Builder {

    public abstract Builder name(String s);

    public abstract Builder packageName(String s);

    public abstract Builder system(boolean b);

    public abstract Builder locked(boolean b);

    public abstract AppEntry build();
  }
}
