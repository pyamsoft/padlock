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

package com.pyamsoft.padlock.dagger.list;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.list.LockInfoAdapter;
import com.pyamsoft.padlock.model.ActivityEntry;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;

final class ActivityEntryAdapterPresenter
    extends AdapterPresenter<ActivityEntry, LockInfoAdapter.ViewHolder> {

  @Inject
  public ActivityEntryAdapterPresenter(@NonNull AdapterInteractor<ActivityEntry> adapterInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(adapterInteractor, mainScheduler, ioScheduler);
  }

  @Override public void setLocked(int position, ActivityEntry.ActivityLockState state) {
    set(position, ActivityEntry.builder(get(position)).lockState(state).build());
  }

  @Override public void setLocked(int position, boolean locked) {
    throw new RuntimeException("Do not use this function here");
  }
}
