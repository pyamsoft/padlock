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

package com.pyamsoft.padlock.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.view.View;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.cache.PersistentCache;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import timber.log.Timber;

public class SettingsFragment extends ActionBarSettingsPreferenceFragment
    implements SettingsPreferencePresenter.SettingsPreferenceView {

  @NonNull public static final String TAG = "SettingsPreferenceFragment";
  @NonNull private static final String KEY_PRESENTER = TAG + "key_settings_presenter";
  @SuppressWarnings("WeakerAccess") SettingsPreferencePresenter presenter;

  @NonNull @Override protected AboutLibrariesFragment.BackStackState isLastOnBackStack() {
    return AboutLibrariesFragment.BackStackState.LAST;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    presenter =
        PersistentCache.load(getActivity(), KEY_PRESENTER, new SettingsPreferencePresenterLoader());
  }

  @CheckResult @NonNull SettingsPreferencePresenter getPresenter() {
    if (presenter == null) {
      throw new NullPointerException("Presenter is NULL");
    }

    return presenter;
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final Preference clearDb = findPreference(getString(R.string.clear_db_key));
    clearDb.setOnPreferenceClickListener(preference -> {
      Timber.d("Clear DB onClick");
      presenter.requestClearDatabase();
      return true;
    });

    final Preference installListener = findPreference(getString(R.string.install_listener_key));
    installListener.setOnPreferenceClickListener(preference -> {
      presenter.setApplicationInstallReceiverState();
      return true;
    });
  }

  @Override protected int getPreferenceXmlResId() {
    return R.xml.preferences;
  }

  @Override protected int getRootViewContainer() {
    return R.id.main_view_container;
  }

  @NonNull @Override protected String getApplicationName() {
    return getString(R.string.app_name);
  }

  @Override protected boolean onClearAllPreferenceClicked() {
    presenter.requestClearAll();
    return true;
  }

  @Override protected boolean onLicenseItemClicked() {
    MainActivity.getNavigationDrawerController(getActivity()).drawerShowUpNavigation();
    return super.onLicenseItemClicked();
  }

  @Override public void showConfirmDialog(int type) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        ConfirmationDialog.newInstance(type), "confirm_dialog");
  }

  @Override public void onClearAll() {
    Timber.d("Everything is cleared, kill self");
    try {
      PadLockService.finish();
    } catch (NullPointerException e) {
      Timber.e(e, "Expected NPE when Service is NULL");
    }
    final ActivityManager activityManager = (ActivityManager) getContext().getApplicationContext()
        .getSystemService(Context.ACTIVITY_SERVICE);
    activityManager.clearApplicationUserData();
  }

  @Override public void onClearDatabase() {
    final Activity activity = getActivity();
    if (activity instanceof MainActivity) {
      ((MainActivity) activity).forceRefresh();
    } else {
      throw new ClassCastException("Activity is not MainActivity");
    }
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(this);
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(true);
    MainActivity.getNavigationDrawerController(getActivity()).drawerNormalNavigation();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    PadLock.getRefWatcher(this).watch(this);
  }
}
