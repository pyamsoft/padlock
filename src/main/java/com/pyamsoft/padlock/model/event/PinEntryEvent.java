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

import com.google.auto.value.AutoValue;

@AutoValue public abstract class PinEntryEvent {

  public static PinEntryEvent.Builder builder() {
    return new AutoValue_PinEntryEvent.Builder();
  }

  // --Commented out by Inspection START (4/30/16 2:14 PM):
  //  public static PinEntryEvent.Builder builder(PinEntryEvent event) {
  //    return new AutoValue_PinEntryEvent.Builder(event);
  //  }
  // --Commented out by Inspection STOP (4/30/16 2:14 PM)

  public abstract int type();

  public abstract boolean complete();

  @AutoValue.Builder public static abstract class Builder {

    public abstract Builder type(int type);

    public abstract Builder complete(boolean b);

    public abstract PinEntryEvent build();
  }
}
