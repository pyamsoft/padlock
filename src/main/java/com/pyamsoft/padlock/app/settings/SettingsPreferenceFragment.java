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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.bus.MainBus;
import com.pyamsoft.padlock.app.loader.SettingsPreferencePresenterLoader;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.model.event.RefreshEvent;
import com.pyamsoft.pydroid.base.fragment.ActionBarSettingsPreferenceFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import timber.log.Timber;

public final class SettingsPreferenceFragment extends ActionBarSettingsPreferenceFragment
    implements SettingsPreferencePresenter.SettingsPreferenceView {

  @NonNull public static final String TAG = "SettingsPreferenceFragment";
  SettingsPreferencePresenter presenter;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    getLoaderManager().initLoader(0, null,
        new LoaderManager.LoaderCallbacks<SettingsPreferencePresenter>() {
          @Override public Loader<SettingsPreferencePresenter> onCreateLoader(int id, Bundle args) {
            return new SettingsPreferencePresenterLoader(getContext());
          }

          @Override public void onLoadFinished(Loader<SettingsPreferencePresenter> loader,
              SettingsPreferencePresenter data) {
            presenter = data;
          }

          @Override public void onLoaderReset(Loader<SettingsPreferencePresenter> loader) {
            presenter = null;
          }
        });
    return super.onCreateView(inflater, container, savedInstanceState);
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

  @Override public void onResume() {
    super.onResume();
    presenter.bindView(this);
  }

  @Override public void onPause() {
    super.onPause();
    presenter.unbindView();
  }
}
