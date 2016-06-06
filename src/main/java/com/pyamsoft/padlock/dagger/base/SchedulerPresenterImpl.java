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

package com.pyamsoft.padlock.dagger.base;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.base.PresenterImpl;
import javax.inject.Named;
import rx.Scheduler;

public abstract class SchedulerPresenterImpl<I> extends PresenterImpl<I> {

  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  protected SchedulerPresenterImpl(@NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @NonNull protected final Scheduler getMainScheduler() {
    return mainScheduler;
  }

  @NonNull protected final Scheduler getIoScheduler() {
    return ioScheduler;
  }
}
