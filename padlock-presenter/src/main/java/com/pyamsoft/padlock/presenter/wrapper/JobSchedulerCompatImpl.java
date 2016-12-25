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

package com.pyamsoft.padlock.presenter.wrapper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import javax.inject.Inject;

class JobSchedulerCompatImpl implements JobSchedulerCompat {

  @NonNull private final Context appContext;
  @NonNull private final AlarmManager alarmManager;

  @Inject JobSchedulerCompatImpl(@NonNull Context context) {
    appContext = context.getApplicationContext();
    alarmManager =
        (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
  }

  @Override public void cancel(@NonNull Intent intent) {
    final PendingIntent pendingIntent =
        PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    alarmManager.cancel(pendingIntent);
    pendingIntent.cancel();
  }

  @Override public void set(@NonNull Intent intent, long triggerTime) {
    alarmManager.set(AlarmManager.RTC, triggerTime,
        PendingIntent.getService(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
  }
}
