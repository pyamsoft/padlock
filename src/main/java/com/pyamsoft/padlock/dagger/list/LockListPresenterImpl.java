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
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.app.lock.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImpl;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockListPresenterImpl extends PresenterImpl<LockListPresenter.LockList>
    implements LockListPresenter {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;
  @NonNull private final Scheduler mainScheduler;
  @NonNull private final Scheduler ioScheduler;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription pinEntryBusSubscription = Subscriptions.empty();
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();

  @Inject public LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor,
      @NonNull @Named("main") Scheduler mainScheduler,
      @NonNull @Named("io") Scheduler ioScheduler) {
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
    this.mainScheduler = mainScheduler;
    this.ioScheduler = ioScheduler;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubscribePopulateList();
  }

  @Override public void onResume() {
    super.onResume();
    registerOnConfirmDialogBus();
    registerOnPinEntryBus();
  }

  @Override public void onPause() {
    super.onPause();
    unregisterFromPinEntryBus();
    unregisterFromConfirmDialogBus();
  }

  private void unsubscribePopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("unsub pop list");
      populateListSubscription.unsubscribe();
    }
  }

  private void setSystemVisible(boolean visible) {
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

    final PackageManager packageManager = lockListInteractor.getPackageManager();

    populateListSubscription = Observable.zip(packageInfoObservable, padlockEntryObservable,
        (applicationInfos, padLockEntries) -> {

          final List<AppEntry> appEntries = new ArrayList<>();
          // KLUDGE super ugly.
          for (final ApplicationInfo applicationInfo : applicationInfos) {
            PadLockEntry foundEntry = null;
            int foundLocation = -1;
            for (int i = 0; i < padLockEntries.size(); ++i) {
              final PadLockEntry padLockEntry = padLockEntries.get(i);
              if (padLockEntry.packageName().equals(applicationInfo.packageName)) {
                foundEntry = padLockEntry;
                foundLocation = i;
                break;
              }
            }

            // Remove any already found entries
            if (foundLocation != -1) {
              padLockEntries.remove(foundLocation);
            }

            final AppEntry appEntry =
                createFromPackageInfo(packageManager, applicationInfo, foundEntry != null);
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
        .subscribeOn(ioScheduler)
        .observeOn(mainScheduler)
        .subscribe(appEntry -> {
          final LockList lockList = getView();
          if (lockList != null) {
            lockList.onEntryAddedToList(appEntry);
          }
        }, throwable -> {
          // TODO handle error
          Timber.e(throwable, "populateList onError");
          final LockList lockList = getView();
          if (lockList != null) {
            lockList.onListPopulated();
          }
        }, () -> {
          final LockList lockList = getView();
          if (lockList != null) {
            lockList.onListPopulated();
          }
        });
  }

  @Nullable
  private AppEntry createFromPackageInfo(PackageManager packageManager, ApplicationInfo info,
      boolean locked) {
    if (info == null) {
      Timber.e("no application info");
      return null;
    }

    Timber.d("Create AppEntry from package info: %s", info.packageName);
    return AppEntry.builder()
        .name(info.loadLabel(packageManager).toString())
        .packageName(info.packageName)
        .system(lockListInteractor.isSystemApplication(info))
        .locked(locked)
        .build();
  }

  @Override public void setFABStateFromPreference() {
    final boolean enabled = stateInteractor.isServiceEnabled();
    final LockList lockList = getView();
    if (lockList != null) {
      if (enabled) {
        lockList.setFABStateEnabled();
      } else {
        lockList.setFABStateDisabled();
      }
    }
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void setSystemVisibilityFromPreference() {
    final boolean visible = lockListInteractor.isSystemVisible();
    final LockList lockList = getView();
    if (lockList != null) {
      if (visible) {
        lockList.setSystemVisible();
      } else {
        lockList.setSystemInvisible();
      }
    }
  }

  @Override public void clickPinFAB() {
    final LockList lockList = getView();
    if (lockList != null) {
      lockList.onPinFABClicked();
    }
  }

  @Override public void showOnBoarding() {
    final boolean onboard = lockListInteractor.hasShownOnBoarding();
    final LockList lockList = getView();
    if (lockList != null) {
      if (!onboard) {
        lockList.showOnBoarding();
      }
    }
  }

  private void registerOnConfirmDialogBus() {
    unregisterFromConfirmDialogBus();
    confirmDialogBusSubscription =
        ConfirmationDialog.ConfirmationDialogBus.get().register().subscribe(confirmationEvent -> {
          Timber.d("Received confirmation event!");
          if (confirmationEvent.type() == 0 && confirmationEvent.complete()) {
            final LockList lockList = getView();
            if (lockList != null) {
              Timber.d("Received database cleared confirmation event, refreshList");
              lockList.refreshList();
            }
          }
        }, throwable -> {
          Timber.e(throwable, "ConfirmationDialogBus onError");
        });
  }

  private void unregisterFromConfirmDialogBus() {
    if (!confirmDialogBusSubscription.isUnsubscribed()) {
      confirmDialogBusSubscription.unsubscribe();
    }
  }

  private void registerOnPinEntryBus() {
    unregisterFromPinEntryBus();
    pinEntryBusSubscription =
        PinEntryDialog.PinEntryBus.get().register().subscribe(pinEntryEvent -> {
          final LockList lockList = getView();
          if (lockList != null) {
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

  @Override public void setOnBoard() {
    lockListInteractor.setShownOnBoarding();
  }
}
