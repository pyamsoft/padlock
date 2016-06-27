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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.PackageManagerWrapper;
import com.pyamsoft.padlock.dagger.base.AppIconLoaderInteractorImpl;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

public class AdapterInteractorImpl<I> extends AppIconLoaderInteractorImpl
    implements AdapterInteractor<I> {

  @NonNull private final List<I> entries;

  @Inject
  protected AdapterInteractorImpl(final @NonNull PackageManagerWrapper packageManagerWrapper) {
    super(packageManagerWrapper);
    this.entries = new ArrayList<>();
  }

  @CheckResult @NonNull @Override public Observable<I> get(int position) {
    return Observable.defer(() -> Observable.just(entries.get(position)));
  }

  @Override public void set(int position, @NonNull I entry) {
    entries.set(position, entry);
  }

  @NonNull @CheckResult @Override public Observable<Integer> add(@NonNull I entry) {
    final int next = entries.size();
    entries.add(next, entry);
    return Observable.defer(() -> Observable.just(next));
  }

  @NonNull @CheckResult @Override public Observable<Integer> remove() {
    final int old = entries.size() - 1;
    entries.remove(old);
    return Observable.defer(() -> Observable.just(old));
  }

  @NonNull @CheckResult @Override public Observable<Integer> size() {
    return Observable.defer(() -> Observable.just(entries.size()));
  }
}
