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

package com.pyamsoft.padlock.dagger.purge;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.purge.PurgePresenter;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import javax.inject.Inject;
import rx.Scheduler;

class PurgePresenterImpl extends SchedulerPresenter<PurgePresenter.View> implements PurgePresenter {

  @NonNull private final PurgeInteractor interactor;

  @Inject PurgePresenterImpl(@NonNull PurgeInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }
}
