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

package com.pyamsoft.padlock.dagger.receiver;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.app.receiver.ApplicationInstallReceiver;
import com.pyamsoft.padlock.dagger.wrapper.PackageManagerWrapper;
import com.pyamsoft.pydroidrx.SchedulerHelper;
import javax.inject.Inject;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

class ApplicationInstallReceiverImpl extends BroadcastReceiver
    implements ApplicationInstallReceiver {

  @NonNull private final Context appContext;
  @NonNull private final NotificationManagerCompat notificationManager;
  @NonNull private final PendingIntent pendingIntent;
  @NonNull private final PackageManagerWrapper packageManagerWrapper;
  @NonNull private final IntentFilter filter;
  @NonNull private final Scheduler obsScheduler;
  @NonNull private final Scheduler subScheduler;
  private int notificationId;
  private boolean registered;
  @NonNull private Subscription notification = Subscriptions.empty();

  @Inject ApplicationInstallReceiverImpl(@NonNull Context context,
      @NonNull PackageManagerWrapper packageManagerWrapper, @NonNull Scheduler obsScheduler,
      @NonNull Scheduler subScheduler) {
    this.obsScheduler = obsScheduler;
    this.subScheduler = subScheduler;
    appContext = context.getApplicationContext();
    this.packageManagerWrapper = packageManagerWrapper;
    filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
    filter.addDataScheme("package");
    registered = false;
    notificationId = 0;
    notificationManager = NotificationManagerCompat.from(appContext);
    final Intent mainIntent = new Intent(appContext, MainActivity.class);
    pendingIntent = PendingIntent.getActivity(appContext, 421, mainIntent, 0);

    SchedulerHelper.enforceObserveScheduler(obsScheduler);
    SchedulerHelper.enforceSubscribeScheduler(subScheduler);
  }

  @Override public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      Timber.e("NULL Intent");
      return;
    }

    final boolean isNew = !intent.hasExtra(Intent.EXTRA_REPLACING);
    final Uri data = intent.getData();
    final String packageName = data.getSchemeSpecificPart();
    notification = packageManagerWrapper.loadPackageLabel(packageName)
        .subscribeOn(subScheduler)
        .observeOn(obsScheduler)
        .subscribe(s -> {
          if (isNew) {
            onNewPackageInstalled(packageName, s);
          } else {
            Timber.d("Package updated: %s", packageName);
          }
        }, throwable -> Timber.e(throwable, "onError launching notification for package: %s",
            packageName), this::unsubNotification);
  }

  void onNewPackageInstalled(@NonNull String packageName, @NonNull String name) {
    Timber.i("Package Added: %s", packageName);
    final Notification notification1 =
        new NotificationCompat.Builder(appContext).setContentTitle("Lock New Application")
            .setSmallIcon(R.drawable.ic_notification_lock)
            .setColor(ContextCompat.getColor(appContext, R.color.blue500))
            .setContentText("Click to lock the newly installed application: " + name)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build();
    notificationManager.notify(notificationId++, notification1);
  }

  @SuppressWarnings("WeakerAccess") void unsubNotification() {
    if (!notification.isUnsubscribed()) {
      notification.unsubscribe();
    }
  }

  @Override public void register() {
    if (!registered) {
      appContext.registerReceiver(this, filter);
      registered = true;
    }
  }

  @Override public void unregister() {
    if (registered) {
      appContext.unregisterReceiver(this);
      registered = false;
    }
  }
}
