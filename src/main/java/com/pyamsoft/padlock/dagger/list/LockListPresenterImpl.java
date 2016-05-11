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

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.pyamsoft.padlock.app.list.LockListPresenter;
import com.pyamsoft.padlock.app.pinentry.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.pinentry.PinEntryDialog;
import com.pyamsoft.padlock.app.settings.ConfirmationDialog;
import com.pyamsoft.padlock.dagger.service.LockServiceStateInteractor;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.pydroid.base.PresenterImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

final class LockListPresenterImpl extends PresenterImpl<LockListPresenter.LockList>
    implements LockListPresenter {

  @NonNull private final LockListInteractor lockListInteractor;
  @NonNull private final LockServiceStateInteractor stateInteractor;

  @NonNull private Subscription confirmDialogBusSubscription = Subscriptions.empty();
  @NonNull private Subscription pinEntryBusSubscription = Subscriptions.empty();
  @NonNull private Subscription systemVisibleSubscription = Subscriptions.empty();
  @NonNull private Subscription fabStateSubscription = Subscriptions.empty();
  @NonNull private Subscription populateListSubscription = Subscriptions.empty();
  @NonNull private Subscription onBoardingSubscription = Subscriptions.empty();

  @Inject public LockListPresenterImpl(final @NonNull LockListInteractor lockListInteractor,
      final @NonNull LockServiceStateInteractor stateInteractor) {
    this.lockListInteractor = lockListInteractor;
    this.stateInteractor = stateInteractor;
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unsubscribeSystemVisible();
    unsubscribeFabState();
    unsubscribeOnBoarding();
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

  private void unsubscribeOnBoarding() {
    if (!onBoardingSubscription.isUnsubscribed()) {
      onBoardingSubscription.unsubscribe();
    }
  }

  private void unsubscribePopulateList() {
    if (!populateListSubscription.isUnsubscribed()) {
      Timber.d("unsub pop list");
      populateListSubscription.unsubscribe();
    }
  }

  private void setSystemVisible(boolean visible) {
    unsubscribeSystemVisible();
    systemVisibleSubscription = lockListInteractor.setSystemVisible(visible)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override public void populateList() {
    Timber.d("populateList");
    unsubscribePopulateList();

    Timber.d("Get package info list");
    final Observable<List<PackageInfo>> packageInfoObservable =
        lockListInteractor.getPackageInfoList();

    Timber.d("Get padlock entry list");
    final Observable<List<PadLockEntry>> padlockEntryObservable =
        lockListInteractor.getAppEntryList();

    final PackageManager packageManager = lockListInteractor.getPackageManager();
    final Drawable defaultIcon = packageManager.getDefaultActivityIcon();

    populateListSubscription = Observable.zip(packageInfoObservable, padlockEntryObservable,
        (packageInfos, padLockEntries) -> {

          final List<AppEntry> appEntries = new ArrayList<>();
          // KLUDGE super ugly.
          for (final PackageInfo packageInfo : packageInfos) {
            PadLockEntry foundEntry = null;
            int foundLocation = -1;
            for (int i = 0; i < padLockEntries.size(); ++i) {
              final PadLockEntry padLockEntry = padLockEntries.get(i);
              if (padLockEntry.packageName().equals(packageInfo.packageName)) {
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
                createFromPackageInfo(packageManager, packageInfo, defaultIcon, foundEntry != null);
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
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
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

  @Nullable private AppEntry createFromPackageInfo(PackageManager packageManager, PackageInfo info,
      Drawable defaultIcon, boolean locked) {
    Drawable icon;
    final ApplicationInfo appInfo = info.applicationInfo;
    if (appInfo == null) {
      Timber.e("no application info");
      return null;
    }
    if (appInfo.icon != 0) {
      icon = appInfo.loadIcon(packageManager);
    } else {
      icon = defaultIcon;
    }

    PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo(info.packageName, PackageManager.GET_ACTIVITIES);
    } catch (PackageManager.NameNotFoundException e) {
      Timber.e(e, "ERROR in createFromPackageInfo");
      packageInfo = null;
    }

    List<ActivityInfo> infoList;
    if (packageInfo == null) {
      infoList = new ArrayList<>();
    } else {
      final ActivityInfo[] infos = packageInfo.activities;
      if (infos == null) {
        infoList = new ArrayList<>();
      } else {
        infoList = Arrays.asList(infos);
      }
    }

    Timber.d("Create AppEntry from package info: %s", info.packageName);
    return AppEntry.builder()
        .name(appInfo.loadLabel(packageManager).toString())
        .packageName(info.packageName)
        .activities(infoList)
        .icon(((BitmapDrawable) icon).getBitmap())
        .system(lockListInteractor.isSystemApplication(appInfo))
        .locked(locked)
        .build();
  }

  @Override public void setFABStateFromPreference() {
    unsubscribeFabState();
    fabStateSubscription =
        Observable.defer(() -> Observable.just(stateInteractor.isServiceEnabled()))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(bool -> {
              final LockList lockList = getView();
              if (lockList != null) {
                if (bool) {
                  lockList.setFABStateEnabled();
                } else {
                  lockList.setFABStateDisabled();
                }
              }
            }, throwable -> {
              Timber.e(throwable, "setFABStateFromPreference onError");
            });
  }

  private void unsubscribeFabState() {
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

  private void unsubscribeSystemVisible() {
    if (!systemVisibleSubscription.isUnsubscribed()) {
      systemVisibleSubscription.unsubscribe();
    }
  }

  @Override public void setSystemVisibilityFromPreference() {
    unsubscribeSystemVisible();
    systemVisibleSubscription = lockListInteractor.isSystemVisible()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(bool -> {
          final LockList lockList = getView();
          if (lockList != null) {
            if (bool) {
              lockList.setSystemVisible();
            } else {
              lockList.setSystemInvisible();
            }
          }
        }, throwable -> {
          Timber.e(throwable, "setSystemVisiblityFromPreference onError");
        });
  }

  @Override public void clickPinFAB() {
    final LockList lockList = getView();
    if (lockList != null) {
      lockList.onPinFABClicked();
    }
  }

  @Override public void showOnBoarding() {
    unsubscribeOnBoarding();
    onBoardingSubscription = lockListInteractor.hasShownOnBoarding()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(onboard -> {
          final LockList lockList = getView();
          if (lockList != null) {
            if (!onboard) {
              lockList.showOnBoarding();
            }
          }
        }, throwable -> {
          // TODO handle error
          Timber.e(throwable, "onError");
        });
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
    unsubscribeOnBoarding();
    onBoardingSubscription = lockListInteractor.setShownOnBoarding()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }
}
