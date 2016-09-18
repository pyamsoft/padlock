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
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.bus.DBProgressBus;
import com.pyamsoft.padlock.bus.LockInfoDisplayBus;
import com.pyamsoft.padlock.bus.PinEntryBus;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.dagger.presenter.SchedulerPresenter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class LockListPresenterImpl extends SchedulerPresenter<LockListPresenter.LockList>
    implements LockListPresenter {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;

  @NonNull private Subscription pinEntryBusSubscription = Subscriptions.empty();
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();
  @NonNull private Subscription systemVisibleSubscription = Subscriptions.empty();
  @NonNull private Subscription onboardSubscription = Subscriptions.empty();
  @NonNull private Subscription fabStateSubscription = Subscriptions.empty();
  @NonNull private Subscription dbProgressBus = Subscriptions.empty();
  @NonNull private Subscription lockInfoDisplayBus = Subscriptions.empty();
  @NonNull private Subscription databaseSubscription = Subscriptions.empty();
  @NonNull private Subscription zeroActivitySubscription = Subscriptions.empty();

  @Inject LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    super(mainScheduler, ioScheduler);
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override protected void onBind() {
    super.onBind();
    registerOnPinEntryBus();
    registerOnDbProgressBus();
    registerOnLockInfoDisplayBus();
  }

  @Override protected void onUnbind() {
    super.onUnbind();
    unregisterFromPinEntryBus();
    unregisterFromDBProgressBus();
    unregisterFromLockInfoDispalyBus();
    unsubscribePopulateList();
    unsubscribeSystemVisible();
    unsubscribeZeroActivity();
    unsubscribeOnboard();
    unsubscribeFabSubscription();
    unsubDatabaseSubscription();
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnLockInfoDisplayBus() {
    unregisterFromLockInfoDispalyBus();
    lockInfoDisplayBus = LockInfoDisplayBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockInfoDisplayEvent -> {
          getView(lockList -> lockList.displayLockInfoDialog(lockInfoDisplayEvent.entry()));
        }, throwable -> {
          Timber.e(throwable, "onError registerOnLockInfoDisplayBus");
        });
  }

  private void unregisterFromLockInfoDispalyBus() {
    if (!lockInfoDisplayBus.isUnsubscribed()) {
      lockInfoDisplayBus.unsubscribe();
    }
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnDbProgressBus() {
    unregisterFromDBProgressBus();
    dbProgressBus = DBProgressBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(dbProgressEvent -> {
          getView(lockList -> lockList.processDatabaseModifyEvent(dbProgressEvent.isChecked(),
              dbProgressEvent.position(), dbProgressEvent.entry()));
        }, throwable -> {
          Timber.e(throwable, "onError registerOnDbProgressBus");
        });
  }

  private void unregisterFromDBProgressBus() {
    if (!dbProgressBus.isUnsubscribed()) {
      dbProgressBus.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubscribePopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      populateListSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeSystemVisible() {
    if (!systemVisibleSubscription.isUnsubscribed()) {
      systemVisibleSubscription.unsubscribe();
    }
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeOnboard() {
    if (!onboardSubscription.isUnsubscribed()) {
      onboardSubscription.unsubscribe();
    }
  }

  @Override public void populateList() {
    Timber.d("populateList");
    unsubscribePopulateList();

    Timber.d("Get package info list");
    final Observable<List<String>> packageInfoObservable =
        lockListInteractor.getApplicationInfoList();

    Timber.d("Get padlock entry list");
    final Observable<List<PadLockEntry.AllEntries>> padlockEntryObservable =
        lockListInteractor.getAppEntryList();

    populateListSubscription = Observable.zip(packageInfoObservable, padlockEntryObservable,
        (packageNames, padLockEntries) -> {

          // KLUDGE super ugly.
          final List<AppEntry> appEntries = new ArrayList<>();
          for (final String applPackageName : packageNames) {
            int foundLocation = -1;
            for (int i = 0; i < padLockEntries.size(); ++i) {
              final PadLockEntry.AllEntries padLockEntry = padLockEntries.get(i);
              if (padLockEntry.packageName().equals(applPackageName) && padLockEntry.activityName()
                  .equals(PadLockEntry.PACKAGE_ACTIVITY_NAME)) {
                foundLocation = i;
                break;
              }
            }

            // Remove any already found entries
            PadLockEntry.AllEntries foundEntry;
            if (foundLocation != -1) {
              foundEntry = padLockEntries.get(foundLocation);
              padLockEntries.remove(foundLocation);
            } else {
              foundEntry = null;
            }

            final AppEntry appEntry =
                lockListInteractor.createFromPackageInfo(applPackageName, foundEntry != null);
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
        .subscribe(appEntry -> getView(lockList -> lockList.onEntryAddedToList(appEntry)),
            throwable -> {
              Timber.e(throwable, "populateList onError");
              getView(LockList::onListPopulated);
            }, () -> {
              getView(LockList::onListPopulated);
              unsubscribePopulateList();
            });
  }

  @Override public void setFABStateFromPreference() {
    unsubscribeFabSubscription();
    fabStateSubscription = stateInteractor.isServiceEnabled()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(enabled -> getView(lockList -> {
          if (enabled) {
            lockList.setFABStateEnabled();
          } else {
            lockList.setFABStateDisabled();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          // TODO  different error
          getView(LockList::onListPopulateError);
        }, this::unsubscribeFabSubscription);
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeFabSubscription() {
    if (!fabStateSubscription.isUnsubscribed()) {
      fabStateSubscription.unsubscribe();
    }
  }

  @Override public void setSystemVisible() {
    lockListInteractor.setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    lockListInteractor.setSystemVisible(false);
  }

  @Override public void setSystemVisibilityFromPreference() {
    unsubscribeSystemVisible();
    systemVisibleSubscription = lockListInteractor.isSystemVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> getView(lockList -> {
          if (visible) {
            lockList.setSystemVisible();
          } else {
            lockList.setSystemInvisible();
          }
        }), throwable -> {
          // TODO different error
          getView(LockList::onListPopulateError);
        }, this::unsubscribeSystemVisible);
  }

  @Override public void setZeroActivityHidden() {
    lockListInteractor.setZeroActivityVisible(false);
  }

  @Override public void setZeroActivityShown() {
    lockListInteractor.setZeroActivityVisible(true);
  }

  @Override public void setZeroActivityFromPreference() {
    unsubscribeZeroActivity();
    zeroActivitySubscription = lockListInteractor.isZeroActivityVisible()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(visible -> getView(lockList -> {
          if (visible) {
            lockList.setZeroActivityShown();
          } else {
            lockList.setZeroActivityHidden();
          }
        }), throwable -> {
          // TODO different error
          getView(LockList::onListPopulateError);
        }, this::unsubscribeZeroActivity);
  }

  @SuppressWarnings("WeakerAccess") void unsubscribeZeroActivity() {
    if (!zeroActivitySubscription.isUnsubscribed()) {
      zeroActivitySubscription.unsubscribe();
    }
  }

  @Override public void clickPinFAB() {
    getView(lockList -> {
      if (PadLockService.isRunning()) {
        lockList.onCreatePinDialog();
      } else {
        lockList.onCreateAccessibilityDialog();
      }
    });
  }

  @Override public void showOnBoarding() {
    unsubscribeOnboard();
    onboardSubscription = lockListInteractor.hasShownOnBoarding()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(onboard -> getView(lockList -> {
          if (!onboard) {
            lockList.showOnBoarding();
          }
        }), throwable -> {
          Timber.e(throwable, "onError");
          getView(LockList::onListPopulateError);
        }, this::unsubscribeOnboard);
  }

  @VisibleForTesting @SuppressWarnings("WeakerAccess") void registerOnPinEntryBus() {
    unregisterFromPinEntryBus();
    pinEntryBusSubscription = PinEntryBus.get()
        .register()
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(pinEntryEvent -> {
          getView(lockList -> {
            switch (pinEntryEvent.type()) {
              case 0:
                if (pinEntryEvent.complete()) {
                  lockList.onCreateMasterPinSuccess();
                } else {
                  lockList.onCreateMasterPinFailure();
                }
                break;
              case 1:
                if (pinEntryEvent.complete()) {
                  lockList.onClearMasterPinSuccess();
                } else {
                  lockList.onClearMasterPinFailure();
                }
            }
          });
        }, throwable -> {
          Timber.e(throwable, "PinEntryBus onError");
        });
  }

  private void unregisterFromPinEntryBus() {
    if (!pinEntryBusSubscription.isUnsubscribed()) {
      pinEntryBusSubscription.unsubscribe();
    }
  }

  @Override public void setOnBoard() {
    lockListInteractor.setShownOnBoarding();
  }

  @Override
  public void modifyDatabaseEntry(boolean isChecked, int position, @NonNull String packageName,
      @Nullable String code, boolean system) {
    unsubDatabaseSubscription();

    // No whitelisting for modifications from the List
    databaseSubscription = lockListInteractor.modifySingleDatabaseEntry(isChecked, packageName,
        PadLockEntry.PACKAGE_ACTIVITY_NAME, code, system, false, false)
        .subscribeOn(getSubscribeScheduler())
        .observeOn(getObserveScheduler())
        .subscribe(lockState -> {
          switch (lockState) {
            case DEFAULT:
              getView(lockList -> lockList.onDatabaseEntryDeleted(position));
              break;
            case LOCKED:
              getView(lockList -> lockList.onDatabaseEntryCreated(position));
              break;
            case WHITELISTED:
              throw new RuntimeException("Whitelist results are not handled");
          }
        }, throwable -> {
          Timber.e(throwable, "onError modifyDatabaseEntry");
          getView(lockList -> lockList.onDatabaseEntryError(position));
        }, this::unsubDatabaseSubscription);
  }

  @SuppressWarnings("WeakerAccess") void unsubDatabaseSubscription() {
    if (!databaseSubscription.isUnsubscribed()) {
      databaseSubscription.isUnsubscribed();
    }
  }
}
