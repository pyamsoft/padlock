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

package com.pyamsoft.padlock;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import timber.log.Timber;

public class PadLock extends PadLockBase {

  private PadLockComponent component;

  @Override public void onCreate() {
    super.onCreate();
    Timber.w("CREATE NEW PADLOCK APPLICATION");
    component = DaggerPadLockComponent.builder()
        .padLockModule(new PadLockModule(getApplicationContext()))
        .build();
  }

  @NonNull @Override PadLockComponent provideComponent() {
    return component;
  }
}
