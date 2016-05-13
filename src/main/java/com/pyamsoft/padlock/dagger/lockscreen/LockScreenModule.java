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

package com.pyamsoft.padlock.dagger.lockscreen;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lockscreen.LockScreenPresenter;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;

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

  @Provides LockScreenPresenter provideLockScreenPresenter(
      final LockScreenPresenterImpl presenter) {
    return presenter;
  }

  @Provides LockScreenInteractor provideLockScreenInteractor(
      final LockScreenInteractorImpl interactor) {
    return interactor;
  }

  @Provides @Named("ignore_default") long provideIgnoreDefault() {
    return defaultIgnoreTime;
  }

  @Provides @Named("ignore_none") long provideIgnoreNone() {
    return ignoreTimeNone;
  }

  @Provides @Named("ignore_five") long provideIgnoreFive() {
    return ignoreTimeFive;
  }

  @Provides @Named("ignore_ten") long provideIgnoreTen() {
    return ignoreTimeTen;
  }

  @Provides @Named("ignore_thirty") long provideIgnoreThirty() {
    return ignoreTimeThirty;
  }
}
