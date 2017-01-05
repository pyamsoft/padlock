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

package com.pyamsoft.padlock.base;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import com.pyamsoft.padlock.base.db.PadLockDB;
import com.pyamsoft.padlock.base.db.PadLockDBModule;
import com.pyamsoft.padlock.base.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.base.receiver.ReceiverModule;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompat;
import com.pyamsoft.padlock.base.wrapper.JobSchedulerCompatModule;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapper;
import com.pyamsoft.padlock.base.wrapper.PackageManagerWrapperModule;
import dagger.Component;
import javax.inject.Named;
import javax.inject.Singleton;
import rx.Scheduler;

@Singleton @Component(modules = {
    PadLockModule.class, PackageManagerWrapperModule.class, JobSchedulerCompatModule.class,
    PadLockDBModule.class, ReceiverModule.class
}) public interface PadLockComponent {

  PadLockPreferences providePreferences();

  Context provideContext();

  @Named("main") Class<? extends Activity> provideMainActivityClass();

  @Named("lockscreen") Class<? extends Activity> provideLockScreenActivityClas();

  @Named("recheck") Class<? extends IntentService> provideRecheckServiceClass();

  @Named("sub") Scheduler provideSubScheduler();

  @Named("io") Scheduler provideIOScheduler();

  @Named("obs") Scheduler provideObsScheduler();

  JobSchedulerCompat provideJobSchedulerCompat();

  PackageManagerWrapper providePackageManagerWrapper();

  PadLockDB providePadLockDb();

  ApplicationInstallReceiver provideApplicationInstallReceiver();
}
