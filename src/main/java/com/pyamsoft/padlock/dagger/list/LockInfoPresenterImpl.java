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
import com.pyamsoft.padlock.app.iconloader.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.list.LockInfoPresenter;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.presenter.SchedulerPresenter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockInfoPresenterImpl extends SchedulerPresenter<LockInfoPresenter.LockInfoView>
    implements LockInfoPresenter {

  @NonNull private final LockInfoInteractor lockInfoInteractor;
  @NonNull private final AppIconLoaderPresenter<LockInfoView> iconLoader;
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();
  @NonNull private Subscription allInDBSubscription = Subscriptions.empty();

  @Inject LockInfoPresenterImpl(@NonNull AppIconLoaderPresenter<LockInfoView> iconLoader,
      final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.iconLoader = iconLoader;
    this.lockInfoInteractor = lockInfoInteractor;
  }

  @SuppressWarnings("WeakerAccess") @CheckResult
  static int findEntryInActivities(@NonNull List<PadLockEntry> padLockEntries,
      @NonNull String name) {
    int foundLocation = -1;
    for (int i = 0; i < padLockEntries.size(); ++i) {
      final PadLockEntry padLockEntry = padLockEntries.get(i);
      if (padLockEntry.activityName().equals(name)) {
        foundLocation = i;
        break;
      }
    }

    return foundLocation;
  }

  @Override protected void onBind(@NonNull LockInfoView view) {
    super.onBind(view);
    iconLoader.bindView(view);
  }

  @Override protected void onUnbind(@NonNull LockInfoView view) {
    super.onUnbind(view);
    iconLoader.unbindView();
    unsubPopulateList();
    unsubAllInDB();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    iconLoader.destroyView();
  }

  @Override public void populateList(@NonNull String packageName) {
    unsubPopulateList();

    // Filter out the lockscreen and crashlog entries
    final Observable<List<String>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName).toList();

    // Zip together the lists into a list of ActivityEntry objects
    populateListSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              final List<ActivityEntry> entries = new ArrayList<>();
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final String name : activityInfos) {
                final int foundLocation = findEntryInActivities(padLockEntries, name);

                // Remove foundEntry from the list as it is already used
                PadLockEntry foundEntry;
                if (foundLocation != -1) {
                  foundEntry = padLockEntries.get(foundLocation);
                  padLockEntries.remove(foundLocation);
                } else {
                  foundEntry = null;
                }

                ActivityEntry.ActivityLockState state;
                if (foundEntry == null) {
                  state = ActivityEntry.ActivityLockState.DEFAULT;
                } else {
                  if (foundEntry.whitelist()) {
                    state = ActivityEntry.ActivityLockState.WHITELISTED;
                  } else {
                    state = ActivityEntry.ActivityLockState.LOCKED;
                  }
                }
                final ActivityEntry activityEntry =
                    ActivityEntry.builder().lockState(state).name(name).build();
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
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
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
              unsubPopulateList();
            });
  }

  @SuppressWarnings("WeakerAccess") void unsubPopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("Unsub from populate List event");
      populateListSubscription.unsubscribe();
    }
  }

  @Override public void setToggleAllState(@NonNull String packageName) {
    unsubAllInDB();

    // Filter out the lockscreen and crashlog entries
    final Observable<List<String>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName).toList();

    // Zip together the lists into a list of ActivityEntry objects
    allInDBSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              int count = 0;
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final String name : activityInfos) {
                final int foundLocation = findEntryInActivities(padLockEntries, name);

                // Remove foundEntry from the list as it is already used
                if (foundLocation != -1) {
                  padLockEntries.remove(foundLocation);
                  ++count;
                }
              }

              return count == activityInfos.size();
            })
            .subscribeOn(getSubscribeScheduler())
            .observeOn(getObserveScheduler())
            .subscribe(allIn -> {
              if (allIn) {
                getView().enableToggleAll();
              } else {
                getView().disableToggleAll();
              }
            }, throwable -> {
              Timber.e(throwable, "onError");
              // TODO maybe different error
              getView().onListPopulateError();
            }, this::unsubAllInDB);
  }

  @SuppressWarnings("WeakerAccess") void unsubAllInDB() {
    if (!allInDBSubscription.isUnsubscribed()) {
      allInDBSubscription.unsubscribe();
    }
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    iconLoader.loadApplicationIcon(packageName);
  }
}
