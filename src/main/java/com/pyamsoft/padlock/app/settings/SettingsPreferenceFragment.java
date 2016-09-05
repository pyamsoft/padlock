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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.View;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.bus.MainBus;
import com.pyamsoft.padlock.model.event.RefreshEvent;
import com.pyamsoft.pydroid.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.app.fragment.ActionBarSettingsPreferenceFragment;
import com.pyamsoft.pydroid.base.PersistLoader;
import com.pyamsoft.pydroid.model.Licenses;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import timber.log.Timber;

public class SettingsPreferenceFragment extends ActionBarSettingsPreferenceFragment
    implements SettingsPreferencePresenter.SettingsPreferenceView {

  @NonNull public static final String TAG = "SettingsPreferenceFragment";
  @NonNull private static final String KEY_SETTINGS = "key_settings";
  @SuppressWarnings("WeakerAccess") SettingsPreferencePresenter presenter;
  private long loadedKey;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    loadedKey = PersistentCache.load(KEY_SETTINGS, savedInstanceState,
        new PersistLoader.Callback<SettingsPreferencePresenter>() {
          @NonNull @Override public PersistLoader<SettingsPreferencePresenter> createLoader() {
            return new SettingsPreferencePresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull SettingsPreferencePresenter persist) {
            presenter = persist;
          }
        });
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final Preference clearDb = findPreference(getString(R.string.clear_db_key));
    clearDb.setOnPreferenceClickListener(preference -> {
      Timber.d("Clear DB onClick");
      presenter.requestClearDatabase();
      return true;
    });

    final Preference resetAll = findPreference(getString(R.string.clear_all_key));
    resetAll.setOnPreferenceClickListener(preference -> {
      Timber.d("Reset settings onClick");
      presenter.requestClearAll();
      return true;
    });

    final Preference upgradeInfo = findPreference(getString(R.string.upgrade_info_key));
    upgradeInfo.setOnPreferenceClickListener(preference -> showChangelog());

    final SwitchPreferenceCompat showAds =
        (SwitchPreferenceCompat) findPreference(getString(R.string.adview_key));
    showAds.setOnPreferenceChangeListener((preference, newValue) -> toggleAdVisibility(newValue));

    final Preference showAboutLicenses = findPreference(getString(R.string.about_license_key));
    showAboutLicenses.setOnPreferenceClickListener(
        preference -> showAboutLicensesFragment(R.id.settings_preferences_container,
            AboutLibrariesFragment.Styling.LIGHT, Licenses.ANDROID, Licenses.ANDROID_SUPPORT,
            Licenses.PYDROID, Licenses.GOOGLE_PLAY_SERVICES, Licenses.ANDROID_IN_APP_BILLING,
            Licenses.AUTO_VALUE, Licenses.BUTTERKNIFE, Licenses.DAGGER, Licenses.FAST_ADAPTER,
            Licenses.FAST_ADAPTER, Licenses.FIREBASE, Licenses.LEAK_CANARY, Licenses.RETROFIT2,
            Licenses.SQLBRITE, Licenses.SQLDELIGHT, Licenses.ANDROID_PRIORITY_JOBQUEUE,
            Licenses.RXANDROID, Licenses.RXJAVA));

    final Preference checkVersion = findPreference(getString(R.string.check_version_key));
    checkVersion.setOnPreferenceClickListener(preference -> checkForUpdate());
  }

  @Override public void onCreatePreferences(@Nullable Bundle bundle, @Nullable String s) {
    addPreferencesFromResource(R.xml.preferences);
  }

  @Override public void showConfirmDialog(int type) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        ConfirmationDialog.newInstance(type), "confirm_dialog");
  }

  @Override public void onClearAll() {
    Timber.d("Everything is cleared, kill self");
    PadLockService.finish();
    final ActivityManager activityManager = (ActivityManager) getContext().getApplicationContext()
        .getSystemService(Context.ACTIVITY_SERVICE);
    activityManager.clearApplicationUserData();
  }

  @Override public void onClearDatabase() {
    MainBus.get().post(RefreshEvent.create());
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.unload(loadedKey);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.saveKey(KEY_SETTINGS, outState, loadedKey);
    super.onSaveInstanceState(outState);
  }
}
