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

package com.pyamsoft.padlock.base;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.preference.ClearPreferences;
import com.pyamsoft.padlock.base.preference.InstallListenerPreferences;
import com.pyamsoft.padlock.base.preference.LockListPreferences;
import com.pyamsoft.padlock.base.preference.LockScreenPreferences;
import com.pyamsoft.padlock.base.preference.MasterPinPreferences;
import com.pyamsoft.padlock.base.preference.OnboardingPreferences;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Named;
import javax.inject.Singleton;

@Module public class PadLockModule {

  @NonNull private final Context appContext;
  @NonNull private final PadLockPreferencesImpl preferences;
  @NonNull private final Class<? extends IntentService> recheckServiceClass;
  @NonNull private final Class<? extends Activity> mainActivityClass;
  @NonNull private final Class<? extends Activity> lockScreenActivityClass;

  public PadLockModule(@NonNull Context context,
      @NonNull Class<? extends Activity> mainActivityClass,
      @NonNull Class<? extends Activity> lockScreenActivityClass,
      @NonNull Class<? extends IntentService> recheckServiceClass) {
    appContext = context.getApplicationContext();
    preferences = new PadLockPreferencesImpl(appContext);
    this.mainActivityClass = mainActivityClass;
    this.lockScreenActivityClass = lockScreenActivityClass;
    this.recheckServiceClass = recheckServiceClass;
  }

  @Singleton @Provides @NonNull Context provideContext() {
    return appContext;
  }

  @Singleton @Provides @NonNull MasterPinPreferences provideMasterPinPreference() {
    return preferences;
  }

  @Singleton @Provides @NonNull ClearPreferences provideClearPreferences() {
    return preferences;
  }

  @Singleton @Provides @NonNull InstallListenerPreferences provideInstallListenerPreferences() {
    return preferences;
  }

  @Singleton @Provides @NonNull LockListPreferences provideLockListPreferences() {
    return preferences;
  }

  @Singleton @Provides @NonNull LockScreenPreferences provideLockScreenPreferences() {
    return preferences;
  }

  @Singleton @Provides @NonNull OnboardingPreferences provideOnboardingPreferences() {
    return preferences;
  }

  @Singleton @Provides @NonNull @Named("main")
  Class<? extends Activity> provideMainActivityClass() {
    return mainActivityClass;
  }

  @Singleton @Provides @NonNull @Named("lockscreen")
  Class<? extends Activity> provideLockScreenActivityClas() {
    return lockScreenActivityClass;
  }

  @Singleton @Provides @NonNull @Named("recheck")
  Class<? extends IntentService> provideRecheckServiceClass() {
    return recheckServiceClass;
  }

  @Singleton @Provides @NonNull @Named("sub") Scheduler provideSubScheduler() {
    return Schedulers.computation();
  }

  @Singleton @Provides @NonNull @Named("io") Scheduler provideIOScheduler() {
    return Schedulers.io();
  }

  @Singleton @Provides @NonNull @Named("obs") Scheduler provideObsScheduler() {
    return AndroidSchedulers.mainThread();
  }
}
