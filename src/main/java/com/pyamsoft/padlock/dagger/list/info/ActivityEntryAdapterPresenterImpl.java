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

package com.pyamsoft.padlock.dagger.list.info;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.list.info.LockInfoAdapter;
import com.pyamsoft.padlock.dagger.list.AdapterInteractor;
import com.pyamsoft.padlock.dagger.list.AdapterPresenterImpl;
import com.pyamsoft.padlock.model.ActivityEntry;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Scheduler;

final class ActivityEntryAdapterPresenterImpl
    extends AdapterPresenterImpl<ActivityEntry, LockInfoAdapter.ViewHolder> {

  @Inject public ActivityEntryAdapterPresenterImpl(
      @NonNull AdapterInteractor<ActivityEntry> adapterInteractor,
      @NonNull @Named("io") Scheduler ioScheduler,
      @NonNull @Named("main") Scheduler mainScheduler) {
    super(adapterInteractor, ioScheduler, mainScheduler);
  }

  @Override public void setLocked(int position, boolean locked) {
    set(position, ActivityEntry.builder(get(position)).locked(locked).build());
  }
}
