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

package com.pyamsoft.padlock.dagger.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.settings.SettingsPresenter;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;

@Module public class SettingsModule {

  private final long defaultTimeoutTime;

  public SettingsModule(final @NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    defaultTimeoutTime = Long.parseLong(appContext.getString(R.string.timeout_time_default));
  }

  @Provides SettingsPresenter provideSettingsPresenter(
      final @NonNull SettingsPresenterImpl presenter) {
    return presenter;
  }

  @Provides SettingsInteractor provideSettingsInteractor(
      final @NonNull SettingsInteractorImpl interactor) {
    return interactor;
  }

  @Provides @Named("timeout_default") long provideTimeoutDefault() {
    return defaultTimeoutTime;
  }
}
