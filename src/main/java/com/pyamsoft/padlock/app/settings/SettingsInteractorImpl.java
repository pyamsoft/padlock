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

package com.pyamsoft.padlock.app.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import javax.inject.Inject;
import timber.log.Timber;

public class SettingsInteractorImpl implements SettingsInteractor {

  @NonNull private final PadLockPreferences preferences;
  @NonNull private final Context appContext;

  @Inject public SettingsInteractorImpl(final @NonNull Context context,
      final @NonNull PadLockPreferences preferences) {
    appContext = context.getApplicationContext();
    this.preferences = preferences;
  }

  @Override public void clearDatabase() {
    Timber.d("Clear database of all entries");
    PadLockDB.with(appContext).delete(PadLockEntry.TABLE_NAME, "1=1");
  }

  @Override public void clearAll() {
    clearDatabase();

    preferences.clear();
  }
}
