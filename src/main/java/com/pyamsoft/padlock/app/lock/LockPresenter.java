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

package com.pyamsoft.padlock.app.lock;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import javax.inject.Named;
import rx.Scheduler;

public abstract class LockPresenter<I extends LockPresenter.LockView>
    extends SchedulerPresenter<I> {

  protected LockPresenter(@NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
  }

  public interface LockView {

    void onSubmitSuccess();

    void onSubmitFailure();

    void onSubmitError();
  }
}
