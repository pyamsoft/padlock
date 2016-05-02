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

package com.pyamsoft.padlock.app.list.info;

import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImplBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class LockInfoPresenterImpl extends PresenterImplBase<LockInfoView>
    implements LockInfoPresenter {

  @NonNull private final LockInfoInteractor lockInfoInteractor;

  @NonNull private Subscription populateListSubscription = Subscriptions.empty();

  @Inject public LockInfoPresenterImpl(final @NonNull LockInfoInteractor lockInfoInteractor) {
    this.lockInfoInteractor = lockInfoInteractor;
  }

  @Override public void destroy() {
    super.destroy();
    unsubPopulateList();
  }

  @Override
  public void populateList(@NonNull String packageName, @NonNull List<ActivityInfo> activities) {
    unsubPopulateList();
    populateListSubscription =
        getListObservable(packageName, activities).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(activityEntries -> {
              // The DB never stops presenting onNexts, take the list and handle it then
              // call the view 'onComplete'
              for (final ActivityEntry entry : activityEntries) {
                Timber.d("LockInfoPresenterImpl populateList onNext");
                get().onEntryAddedToList(entry);
              }

              // Unsub here to prevent continued processing.
              // This fixes the duplicate entries that appear on list click
              Timber.d("LockInfoPresenterImpl populateList onComplete faked");
              get().onListPopulated();
              unsubPopulateList();
            }, throwable -> {
              Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
              get().onListPopulateError();
            });
  }

  private void unsubPopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("Unsub from populate List event");
      populateListSubscription.unsubscribe();
    }
  }

  @NonNull private Observable<List<ActivityEntry>> getListObservable(String packageName,
      List<ActivityInfo> activities) {
    return lockInfoInteractor.getActivityEntries(packageName).map(padLockEntries -> {
      final List<ActivityEntry> entries = new ArrayList<>();
      // KLUDGE super ugly.
      Timber.d("Search set for locked activities");
      for (final ActivityInfo info : activities) {
        Timber.d("Loopy loop");
        PadLockEntry foundEntry = null;
        final String name = info.name;
        for (final PadLockEntry entry : padLockEntries) {
          Timber.d("Inner Loopy loop");
          if (name.equals(entry.activityName())) {
            Timber.d("Entry: ", name, " is locked");
            foundEntry = entry;
            break;
          }
        }

        if (foundEntry != null) {
          Timber.d("Remove found entry from set");
          padLockEntries.remove(foundEntry);
        }

        Timber.d("Add entry to list");
        entries.add(ActivityEntry.builder().locked(foundEntry != null).name(name).build());
      }

      Timber.d("Finished with list parsing");

      // KLUDGE Sorting the list by using concatMap and then toSortedList hangs for some reason
      // on the toSortedList call. Sort in here instead
      Timber.d("Sort in here");
      Collections.sort(entries, (entry, entry2) -> {
        if (entry.name().startsWith(packageName)) {
          return -1;
        } else if (entry2.name().startsWith(packageName)) {
          return 1;
        } else {
          return entry.name().compareToIgnoreCase(entry2.name());
        }
      });

      return entries;
    });
  }
}
