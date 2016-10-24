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
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroidrx.SchedulerPresenter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

class PurgePresenterImpl extends SchedulerPresenter<PurgePresenter.View> implements PurgePresenter {

  @NonNull private final PurgeInteractor interactor;

  @Inject PurgePresenterImpl(@NonNull PurgeInteractor interactor,
      @NonNull Scheduler observeScheduler, @NonNull Scheduler subscribeScheduler) {
    super(observeScheduler, subscribeScheduler);
    this.interactor = interactor;
  }

  @Override public void retrieveOldApplications() {
    interactor.getAppEntryList()
        .zipWith(interactor.getActiveApplicationPackageNames().toList(),
            (allEntries, packageNames) -> {
              final List<String> stalePackageNames = new ArrayList<>();
              // Remove all active applications from the list of entries
              for (final String packageName : packageNames) {
                int foundLocation = -1;
                for (int i = 0; i < allEntries.size(); ++i) {
                  final PadLockEntry.AllEntries entry = allEntries.get(i);
                  if (entry.packageName().equals(packageName)) {
                    foundLocation = i;
                    break;
                  }
                }

                if (foundLocation != -1) {
                  Timber.d("Found entry for %s at %d, remove", packageName, foundLocation);
                  allEntries.remove(foundLocation);
                } else {
                  Timber.w("Package %s not found in database", packageName);
                }
              }

              // The remaining entries in the database are stale
              for (final PadLockEntry.AllEntries entry : allEntries) {
                Timber.i("Stale database entry: %s", entry);
                stalePackageNames.add(entry.packageName());
              }

              return stalePackageNames;
            })
        .concatMap(Observable::from)
        .sorted(String::compareToIgnoreCase).subscribeOn(getSubscribeScheduler()).observeOn(getObserveScheduler())
    .subscribe(new Action1<String>() {
      @Override public void call(String s) {

      }
    }, new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {

      }
    }, new Action0() {
      @Override public void call() {

      }
    });
  }
}
