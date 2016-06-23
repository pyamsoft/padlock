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

package com.pyamsoft.padlock.dagger.lock;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lock.LockScreenPresenter;
import com.pyamsoft.padlock.dagger.ActivityScope;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import rx.Scheduler;

@Module public class LockScreenModule {

  private final long defaultIgnoreTime;
  private final long ignoreTimeNone;
  private final long ignoreTimeFive;
  private final long ignoreTimeTen;
  private final long ignoreTimeThirty;

  public LockScreenModule(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    defaultIgnoreTime = Long.parseLong(appContext.getString(R.string.ignore_time_default));

    final String[] ignoreTimes =
        appContext.getResources().getStringArray(R.array.ignore_time_entries);
    ignoreTimeNone = Long.parseLong(ignoreTimes[0]);
    ignoreTimeFive = Long.parseLong(ignoreTimes[1]);
    ignoreTimeTen = Long.parseLong(ignoreTimes[2]);
    ignoreTimeThirty = Long.parseLong(ignoreTimes[3]);
  }

  @ActivityScope @Provides LockScreenPresenter provideLockScreenPresenter(
      final LockScreenInteractor interactor, @Named("main") Scheduler mainScheduler,
      @Named("io") Scheduler ioScheduler) {
    return new LockScreenPresenter(interactor, mainScheduler, ioScheduler, ignoreTimeNone,
        ignoreTimeFive, ignoreTimeTen, ignoreTimeThirty);
  }

  @ActivityScope @Provides LockScreenInteractor provideLockScreenInteractor(
      final LockScreenInteractorImpl interactor) {
    return interactor;
  }

  @ActivityScope @Provides @Named("ignore_default") long provideIgnoreDefault() {
    return defaultIgnoreTime;
  }

  @ActivityScope @Provides @Named("ignore_none") long provideIgnoreNone() {
    return ignoreTimeNone;
  }

  @ActivityScope @Provides @Named("ignore_five") long provideIgnoreFive() {
    return ignoreTimeFive;
  }

  @ActivityScope @Provides @Named("ignore_ten") long provideIgnoreTen() {
    return ignoreTimeTen;
  }

  @ActivityScope @Provides @Named("ignore_thirty") long provideIgnoreThirty() {
    return ignoreTimeThirty;
  }
}
