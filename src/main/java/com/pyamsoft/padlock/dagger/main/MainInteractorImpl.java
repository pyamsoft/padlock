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

package com.pyamsoft.padlock.dagger.main;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.app.main.MainInteractor;
import javax.inject.Inject;
import rx.Observable;

final class MainInteractorImpl implements MainInteractor {

  @NonNull private final PadLockPreferences preferences;

  @Inject public MainInteractorImpl(final @NonNull PadLockPreferences preferences) {
    this.preferences = preferences;
  }

  @WorkerThread @NonNull @Override public Observable<Boolean> hasAgreed() {
    return Observable.defer(() -> Observable.just(preferences.hasAgreed()))
        .map(aBoolean -> aBoolean == null ? false : aBoolean);
  }

  @NonNull @Override public Observable<Boolean> setAgreed() {
    return Observable.defer(() -> {
      preferences.setAgreed();
      return Observable.just(true);
    });
  }
}
