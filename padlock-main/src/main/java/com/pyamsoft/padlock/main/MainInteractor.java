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

package com.pyamsoft.padlock.main;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.base.preference.OnboardingPreferences;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton class MainInteractor {

  @SuppressWarnings("WeakerAccess") @NonNull final OnboardingPreferences preferences;

  @Inject MainInteractor(final @NonNull OnboardingPreferences preferences) {
    this.preferences = preferences;
  }

  /**
   * public
   */
  @NonNull @CheckResult Single<Boolean> isOnboardingComplete() {
    return Single.fromCallable(preferences::hasAgreed);
  }
}
