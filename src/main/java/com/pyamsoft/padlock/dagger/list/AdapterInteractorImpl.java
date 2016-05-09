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
import java.util.ArrayList;
import java.util.List;

public abstract class AdapterInteractorImpl<I> implements AdapterInteractor<I> {

  @NonNull private final List<I> entries;

  protected AdapterInteractorImpl() {
    this.entries = new ArrayList<>();
  }

  @NonNull @Override public I get(int position) {
    return entries.get(position);
  }

  @Override public void set(int position, I entry) {
    entries.set(position, entry);
  }

  @Override public int add(I entry) {
    final int next = entries.size();
    entries.add(next, entry);
    return next;
  }

  @Override public int remove() {
    final int old = entries.size() - 1;
    entries.remove(old);
    return old;
  }

  @Override public int size() {
    return entries.size();
  }
}
