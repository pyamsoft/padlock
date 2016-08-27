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

import android.content.pm.ApplicationInfo;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.app.lock.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
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

final class LockListPresenterImpl extends SchedulerPresenter<LockListPresenter.LockList>
    implements LockListPresenter {

  @NonNull final LockListInteractor lockListInteractor;
  @NonNull final LockServiceStateInteractor stateInteractor;

  @NonNull Subscription pinEntryBusSubscription = Subscriptions.empty();
  @NonNull Subscription populateListSubscription = Subscriptions.empty();
  @NonNull Subscription systemVisibleSubscription = Subscriptions.empty();
  @NonNull Subscription onboardSubscription = Subscriptions.empty();
  @NonNull Subscription fabStateSubscription = Subscriptions.empty();

  @Inject LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onBind(@NonNull LockList view) {
    super.onBind(view);
    registerOnPinEntryBus(view);
  }

  @Override protected void onUnbind(@NonNull LockList view) {
    super.onUnbind(view);
    unregisterFromPinEntryBus();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    unsubscribePopulateList();
    unsubscribeSystemVisible();
    unsubscribeOnboard();
    unsubscribeFabSubscription();
  }

  void unsubscribePopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      populateListSubscription.unsubscribe();
    }
  }

  void unsubscribeSystemVisible() {
    if (!systemVisibleSubscription.isUnsubscribed()) {
      systemVisibleSubscription.unsubscribe();
    }
  }

  void unsubscribeOnboard() {
    if (!onboardSubscription.isUnsubscribed()) {
      onboardSubscription.unsubscribe();
    }
  }

  void setSystemVisible(boolean visible) {
    lockListInteractor.setSystemVisible(visible);
  }

  @Override public void populateList() {
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
          unsubscribePopulateList();
        });
  }

  @NonNull @CheckResult AppEntry createFromPackageInfo(@NonNull ApplicationInfo info,
      boolean locked) {
    Timber.d("Create AppEntry from package info: %s", info.packageName);
    return AppEntry.builder()
        .name(lockListInteractor.loadPackageLabel(info).toBlocking().first())
        .packageName(info.packageName)
        .system(lockListInteractor.isSystemApplication(info).toBlocking().first())
        .locked(locked)
        .build();
  }

  @Override public void setFABStateFromPreference() {
    unsubscribeFabSubscription();
    fabStateSubscription = stateInteractor.isServiceEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(enabled -> {
          final LockList lockList = getView();
          if (enabled) {
            lockList.setFABStateEnabled();
          } else {
            lockList.setFABStateDisabled();
          }
        }, throwable -> {
          Timber.e(throwable, "onError");
          // TODO  different error
          getView().onListPopulateError();
        }, this::unsubscribeFabSubscription);
  }

  void unsubscribeFabSubscription() {
    if (!fabStateSubscription.isUnsubscribed()) {
      fabStateSubscription.unsubscribe();
    }
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void setSystemVisibilityFromPreference() {
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
        }, this::unsubscribeSystemVisible);
  }

  @Override public void clickPinFAB() {
    final LockList lockList = getView();
    lockList.onPinFABClicked();
  }

  @Override public void showOnBoarding() {
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
          Timber.e(throwable, "onError");
          getView().onListPopulateError();
        }, this::unsubscribeOnboard);
  }

  void registerOnPinEntryBus(@NonNull LockList view) {
    unregisterFromPinEntryBus();
    pinEntryBusSubscription = PinEntryDialog.PinEntryBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          if (view instanceof MasterPinSubmitCallback) {
            final MasterPinSubmitCallback callback = (MasterPinSubmitCallback) view;
            switch (pinEntryEvent.type()) {
              case 0:
                if (pinEntryEvent.complete()) {
                  callback.onCreateMasterPinSuccess();
                } else {
                  callback.onCreateMasterPinFailure();
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

  void unregisterFromPinEntryBus() {
    if (!pinEntryBusSubscription.isUnsubscribed()) {
      pinEntryBusSubscription.unsubscribe();
    }
  }

  @Override public void setOnBoard() {
    lockListInteractor.setShownOnBoarding();
  }
}
