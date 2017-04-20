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

package com.pyamsoft.padlock.lock.common;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.presenter.SchedulerPresenter;
import io.reactivex.Scheduler;
import javax.inject.Inject;
import javax.inject.Named;

public class LockTypePresenter extends SchedulerPresenter {

  @NonNull private final LockTypeInteractor interactor;

  @Inject protected LockTypePresenter(@NonNull LockTypeInteractor interactor,
      @Named("obs") Scheduler obsScheduler, @Named("sub") Scheduler subScheduler) {
    super(obsScheduler, subScheduler);
    this.interactor = interactor;
  }

  @Override protected void onStop() {
    super.onStop();
  }

  public void initializeLockScreenType(@NonNull LockScreenTypeCallback callback) {
    disposeOnStop(interactor.getLockScreenType()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockScreenType -> {
          switch (lockScreenType) {
            case TYPE_PATTERN:
              callback.onTypePattern();
              break;
            case TYPE_TEXT:
              callback.onTypeText();
              break;
            default:
              throw new IllegalStateException("Invalid lock screen type: " + lockScreenType);
          }
        }));
  }

  public interface LockScreenTypeCallback {
    void onTypeText();

    void onTypePattern();
  }
}
