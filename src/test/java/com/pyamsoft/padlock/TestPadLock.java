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

import android.app.Application;
import android.support.annotation.NonNull;
import com.google.firebase.FirebaseApp;
import com.pyamsoft.padlock.dagger.DaggerTestPadLockComponent;
import com.pyamsoft.padlock.dagger.TestPadLockComponent;
import com.pyamsoft.padlock.dagger.TestPadLockModule;
import com.pyamsoft.padlock.dagger.wrapper.TestPackageManagerWrapperModule;

public class TestPadLock extends Application implements IPadLock {

  private TestPadLockComponent component;

  @Override public void onCreate() {
    super.onCreate();
    if (!FirebaseApp.getApps(getApplicationContext()).isEmpty()) {
      component = DaggerTestPadLockComponent.builder()
          .testPackageManagerWrapperModule(new TestPackageManagerWrapperModule())
          .testPadLockModule(new TestPadLockModule(getApplicationContext()))
          .build();
    }
  }

  @SuppressWarnings("unchecked") @NonNull @Override public TestPadLockComponent provideComponent() {
    if (component == null) {
      throw new NullPointerException("TestPadLockComponent is NULL");
    }
    return component;
  }
}
