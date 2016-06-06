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
import com.pyamsoft.padlock.app.list.info.LockInfoPresenter;
import com.pyamsoft.padlock.app.lockscreen.LockScreenActivity;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImpl;
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

final class LockInfoPresenterImpl extends PresenterImpl<LockInfoPresenter.LockInfoView>
    implements LockInfoPresenter {

  @NonNull private final LockInfoInteractor lockInfoInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription populateListSubscription = Subscriptions.empty();
  @NonNull private Subscription loadIconSubscription = Subscriptions.empty();

  @Inject public LockInfoPresenterImpl(final @NonNull LockInfoInteractor lockInfoInteractor,
      final @NonNull @Named("main") Scheduler mainScheduler,
      final @NonNull @Named("io") Scheduler ioScheduler) {
    this.lockInfoInteractor = lockInfoInteractor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubPopulateList();
    unsubLoadIcon();
  }

  @Override public void populateList(@NonNull String packageName) {
    unsubPopulateList();

    // Filter out the lockscreen and crashlog entries
    final Observable<List<ActivityInfo>> activityInfoObservable =
        lockInfoInteractor.getPackageActivities(packageName).filter(activityInfo -> {
          final String name = activityInfo.name;
          return !name.equalsIgnoreCase(LockScreenActivity.class.getName())
              && !name.equalsIgnoreCase(CrashLogActivity.class.getName());
        }).toList();

    // Zip together the lists into a list of ActivityEntry objects
    populateListSubscription =
        Observable.zip(lockInfoInteractor.getActivityEntries(packageName), activityInfoObservable,
            (padLockEntries, activityInfos) -> {
              final List<ActivityEntry> entries = new ArrayList<>();
              // KLUDGE super ugly.
              Timber.d("Search set for locked activities");
              for (final ActivityInfo info : activityInfos) {
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
            .subscribeOn(ioScheduler)
            .observeOn(mainScheduler)
            .subscribe(activityEntry -> {
              final LockInfoView lockInfoView = getView();
              if (lockInfoView != null) {
                lockInfoView.onEntryAddedToList(activityEntry);
              }
            }, throwable -> {
              Timber.e(throwable, "LockInfoPresenterImpl populateList onError");
              final LockInfoView lockInfoView = getView();
              if (lockInfoView != null) {
                lockInfoView.onListPopulateError();
              }
            }, () -> {
              final LockInfoView lockInfoView = getView();
              if (lockInfoView != null) {
                lockInfoView.onListPopulated();
              }
            });
  }

  private void unsubPopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("Unsub from populate List event");
      populateListSubscription.unsubscribe();
    }
  }

  @Override public void loadApplicationIcon(@NonNull String packageName) {
    loadIconSubscription = lockInfoInteractor.loadPackageIcon(packageName)
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(drawable -> {
          final LockInfoView lockInfoView = getView();
          if (lockInfoView != null) {
            lockInfoView.onApplicationIconLoadedSuccess(drawable);
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          final LockInfoView lockInfoView = getView();
          if (lockInfoView != null) {
            lockInfoView.onApplicationIconLoadedError();
          }
        });
  }

  private void unsubLoadIcon() {
    if (!loadIconSubscription.isUnsubscribed()) {
      Timber.d("Unsub from load icon event");
      loadIconSubscription.unsubscribe();
    }
  }
}
