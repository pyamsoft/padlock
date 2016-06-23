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

package com.pyamsoft.padlock.app.list;

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.AppIconLoaderView;
import com.pyamsoft.padlock.app.lock.LockScreenActivity;
import com.pyamsoft.padlock.dagger.list.LockInfoInteractor;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.crash.CrashLogActivity;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockInfoPresenter extends AppIconLoaderPresenter<LockInfoPresenter.LockInfoView> {

  @NonNull private final LockInfoInteractor lockInfoInteractor;
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();

  @Inject public LockInfoPresenter(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    super(lockInfoInteractor, mainScheduler, ioScheduler);
    this.lockInfoInteractor = lockInfoInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubPopulateList();
  }

  public final void populateList(@NonNull String packageName) {
    unsubPopulateList();

    // Filter out the lockscreen and crashlog entries
    final Observable<List<String>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName)
            .filter(
                activityEntry -> !activityEntry.equalsIgnoreCase(LockScreenActivity.class.getName())
                    && !activityEntry.equalsIgnoreCase(CrashLogActivity.class.getName()))
            .toList();

    // Zip together the lists into a list of ActivityEntry objects
    populateListSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              final List<ActivityEntry> entries = new ArrayList<>();
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final String name : activityInfos) {
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
            .subscribeOn(getIoScheduler())
            .observeOn(getMainScheduler())
            .subscribe(activityEntry -> {
              final LockInfoView lockInfoView = getView();
              lockInfoView.onEntryAddedToList(activityEntry);
            }, throwable -> {
              Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
              final LockInfoView lockInfoView = getView();
              lockInfoView.onListPopulateError();
            }, () -> {
              final LockInfoView lockInfoView = getView();
              lockInfoView.onListPopulated();
            });
  }

  private void unsubPopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("Unsub from populate List event");
      populateListSubscription.unsubscribe();
    }
  }


  public interface LockInfoView extends LockListCommon, AppIconLoaderView {

    void onEntryAddedToList(@NonNull ActivityEntry entry);

    void onListPopulated();

    void onListPopulateError();
  }
}
