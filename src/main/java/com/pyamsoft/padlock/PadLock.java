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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.pyamsoft.padlock.dagger.DaggerPadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockComponent;
import com.pyamsoft.padlock.dagger.PadLockModule;
import com.pyamsoft.pydroid.lib.PYDroidApplication;
import timber.log.Timber;

public class PadLock extends PYDroidApplication implements IPadLock {

  private PadLockComponent component;

  @NonNull @CheckResult public static IPadLock get(@NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    if (appContext instanceof IPadLock) {
      return (IPadLock) appContext;
    } else {
      throw new ClassCastException("Cannot cast Application Context to PadLockBase");
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    Timber.w("CREATE NEW PADLOCK APPLICATION");
    if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
      Timber.i("INIT NEW FIREBASE APPLICATION");
      component = DaggerPadLockComponent.builder()
          .padLockModule(new PadLockModule(getApplicationContext()))
          .build();
    }
  }

  @SuppressWarnings("unchecked") @NonNull @Override public PadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("PadLockComponent is NULL");
    }
    return component;
  }
}
