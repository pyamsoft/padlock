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

import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.list.info.LockInfoInteractor;
import com.pyamsoft.padlock.app.list.info.LockInfoPresenter;
import com.pyamsoft.padlock.app.list.info.LockInfoView;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImpl;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockInfoPresenterImpl extends PresenterImpl<LockInfoView>
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
        lockInfoInteractor.getActivityEntries(packageName)
            .map(padLockEntries -> {
              final List<ActivityEntry> entries = new ArrayList<>();
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final ActivityInfo info : activities) {
                final String name = info.name;
                PadLockEntry foundEntry = null;
                int foundLocation = -1;
                for (int i = 0; i < padLockEntries.size(); ++i) {
                  final PadLockEntry padLockEntry = padLockEntries.get(i);
                  if (padLockEntry.activityName().equals(name)) {
                    foundEntry = padLockEntry;
                    foundLocation = i;
                    break;
                  }
                }

                // Remove foundEntry from the list as it is already used
                if (foundLocation != -1) {
                  padLockEntries.remove(foundLocation);
                }

                final ActivityEntry activityEntry =
                    ActivityEntry.builder().locked(foundEntry != null).name(name).build();
                Timber.d("Add ActivityEntry: %s", activityEntry);
                entries.add(activityEntry);
              }

              return entries;
            })
            .flatMap(Observable::from)
            .toSortedList((activityEntry, activityEntry2) -> {
              if (activityEntry.name().startsWith(packageName)) {
                return -1;
              } else if (activityEntry2.name().startsWith(packageName)) {
                return 1;
              } else {
                return activityEntry.name().compareToIgnoreCase(activityEntry2.name());
              }
            })
            .concatMap(Observable::from)
            .filter(activityEntry -> activityEntry != null)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(activityEntry -> get().onEntryAddedToList(activityEntry), throwable -> {
              Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
              get().onListPopulateError();
            }, () -> get().onListPopulated());
  }

  private void unsubPopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("Unsub from populate List event");
      populateListSubscription.unsubscribe();
    }
  }
}
