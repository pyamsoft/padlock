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
import com.pyamsoft.padlock.app.list.AdapterInteractor;
import com.pyamsoft.padlock.app.list.AdapterPresenter;
import com.pyamsoft.pydroid.base.PresenterImpl;

public abstract class AdapterPresenterImpl<I> extends PresenterImpl<AdapterPresenter.AdapterView>
    implements AdapterPresenter<I> {

  @NonNull private final AdapterInteractor<I> adapterInteractor;

  protected AdapterPresenterImpl(@NonNull AdapterInteractor<I> adapterInteractor) {
    this.adapterInteractor = adapterInteractor;
  }

  protected void set(int position, I entry) {
    adapterInteractor.set(position, entry);
  }

  @NonNull @Override public I get(int position) {
    return adapterInteractor.get(position);
  }

  @Override public int add(I entry) {
    return adapterInteractor.add(entry);
  }

  @Override public int remove() {
    return adapterInteractor.remove();
  }

  @Override public int size() {
    return adapterInteractor.size();
  }
}
