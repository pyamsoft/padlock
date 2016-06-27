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

import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.base.SchedulerPresenter;
import com.pyamsoft.padlock.app.lock.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.dagger.list.LockListInteractor;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockListPresenter extends SchedulerPresenter<LockListPresenter.LockList> {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;

  @NonNull private Subscription pinEntryBusSubscription = Subscriptions.empty();
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();
  @NonNull private Subscription systemVisibleSubscription = Subscriptions.empty();
  @NonNull private Subscription onboardSubscription = Subscriptions.empty();

  @Inject public LockListPresenter(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unsubscribePopulateList();
    unsubscribeSystemVisible();
    unsubscribeOnboard();
  }

  @Override public void onResume() {
    super.onResume();
    registerOnPinEntryBus();
  }

  @Override public void onPause() {
    super.onPause();
    unregisterFromPinEntryBus();
  }

  private void unsubscribePopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      populateListSubscription.unsubscribe();
    }
  }

  private void unsubscribeSystemVisible() {
    if (!systemVisibleSubscription.isUnsubscribed()) {
      systemVisibleSubscription.unsubscribe();
    }
  }

  private void unsubscribeOnboard() {
    if (!onboardSubscription.isUnsubscribed()) {
      onboardSubscription.unsubscribe();
    }
  }

  private void setSystemVisible(boolean visible) {
    lockListInteractor.setSystemVisible(visible);
  }

  public final void populateList() {
    Timber.d("populateList");
    unsubscribePopulateList();

    Timber.d("Get package info list");
    final Observable<List<ApplicationInfo>> packageInfoObservable =
        lockListInteractor.getApplicationInfoList();

    Timber.d("Get padlock entry list");
    final Observable<List<PadLockEntry>> padlockEntryObservable =
        lockListInteractor.getAppEntryList();

    populateListSubscription = Observable.zip(packageInfoObservable, padlockEntryObservable,
        (applicationInfos, padLockEntries) -> {

          // KLUDGE super ugly.
          final List<AppEntry> appEntries = new ArrayList<>();
          for (final ApplicationInfo applicationInfo : applicationInfos) {
            int foundLocation = -1;
            for (int i = 0; i < padLockEntries.size(); ++i) {
              final PadLockEntry padLockEntry = padLockEntries.get(i);
              if (padLockEntry.packageName().equals(applicationInfo.packageName)
                  && padLockEntry.activityName().equals(PadLockEntry.PACKAGE_TAG)) {
                foundLocation = i;
                break;
              }
            }

            // Remove any already found entries
            PadLockEntry foundEntry;
            if (foundLocation != -1) {
              foundEntry = padLockEntries.get(foundLocation);
              padLockEntries.remove(foundLocation);
            } else {
              foundEntry = null;
            }

            final AppEntry appEntry = createFromPackageInfo(applicationInfo, foundEntry != null);
            Timber.d("Add AppEntry: %s", appEntry);
            appEntries.add(appEntry);
          }
          return appEntries;
        })
        .flatMap(Observable::from)
        .toSortedList((appEntry, appEntry2) -> {
          return appEntry.name().compareToIgnoreCase(appEntry2.name());
        })
        .concatMap(Observable::from)
        .filter(appEntry -> appEntry != null)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(appEntry -> {
          final LockList lockList = getView();
          lockList.onEntryAddedToList(appEntry);
        }, throwable -> {
          Timber.e(throwable, "populateList onError");
          final LockList lockList = getView();
          lockList.onListPopulated();
        }, () -> {
          final LockList lockList = getView();
          lockList.onListPopulated();
        });
  }

  @NonNull @CheckResult
  private AppEntry createFromPackageInfo(@NonNull ApplicationInfo info, boolean locked) {
    Timber.d("Create AppEntry from package info: %s", info.packageName);
    return AppEntry.builder()
        .name(lockListInteractor.loadPackageLabel(info).toBlocking().first())
        .packageName(info.packageName)
        .system(lockListInteractor.isSystemApplication(info).toBlocking().first())
        .locked(locked)
        .build();
  }

  public final void setFABStateFromPreference() {
    final boolean enabled = stateInteractor.isServiceEnabled();
    final LockList lockList = getView();
    if (enabled) {
      lockList.setFABStateEnabled();
    } else {
      lockList.setFABStateDisabled();
    }
  }

  public final void setSystemVisible() {
    setSystemVisible(true);
  }

  public final void setSystemInvisible() {
    setSystemVisible(false);
  }

  public final void setSystemVisibilityFromPreference() {
    unsubscribeSystemVisible();
    systemVisibleSubscription = lockListInteractor.isSystemVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> {
          final LockList lockList = getView();
          if (visible) {
            lockList.setSystemVisible();
          } else {
            lockList.setSystemInvisible();
          }
        }, throwable -> {
          // TODO different error
          getView().onListPopulateError();
        });
  }

  public final void clickPinFAB() {
    final LockList lockList = getView();
    lockList.onPinFABClicked();
  }

  public final void showOnBoarding() {
    unsubscribeOnboard();
    onboardSubscription = lockListInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> {
          final LockList lockList = getView();
          if (!onboard) {
            lockList.showOnBoarding();
          }
        }, throwable -> {
          // TODO different error
          getView().onListPopulateError();
        });
  }

  private void registerOnPinEntryBus() {
    unregisterFromPinEntryBus();
    pinEntryBusSubscription = PinEntryDialog.PinEntryBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          final LockList lockList = getView();
          if (lockList instanceof MasterPinSubmitCallback) {
            final MasterPinSubmitCallback callback = (MasterPinSubmitCallback) lockList;
            switch (pinEntryEvent.type()) {
              case 0:
                if (pinEntryEvent.complete()) {
                  callback.onCreateMasterPin();
                }
                break;
              case 1:
                if (pinEntryEvent.complete()) {
                  callback.onClearMasterPinSuccess();
                } else {
                  callback.onClearMasterPinFailure();
                }
            }
          }
        }, throwable -> {
          Timber.e(throwable, "PinEntryBus onError");
        });
  }

  private void unregisterFromPinEntryBus() {
    if (!pinEntryBusSubscription.isUnsubscribed()) {
      pinEntryBusSubscription.unsubscribe();
    }
  }

  public final void setOnBoard() {
    lockListInteractor.setShownOnBoarding();
  }

  public interface LockList extends LockListCommon {

    void setFABStateEnabled();

    void setFABStateDisabled();

    void setSystemVisible();

    void setSystemInvisible();

    void onPinFABClicked();

    void onEntryAddedToList(@NonNull AppEntry entry);

    void showOnBoarding();
  }
}
