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

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.view.View;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.main.MainActivity;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarSettingsPreferenceFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class SettingsFragment extends ActionBarSettingsPreferenceFragment
    implements SettingsPreferencePresenter.RequestCallback {

  @NonNull public static final String TAG = "SettingsPreferenceFragment";
  @SuppressWarnings("WeakerAccess") @Inject SettingsPreferencePresenter presenter;

  @NonNull @Override protected AboutLibrariesFragment.BackStackState isLastOnBackStack() {
    return AboutLibrariesFragment.BackStackState.LAST;
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Injector.get().provideComponent().plusSettingsPreferenceComponent().inject(this);
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
      presenter.requestClearDatabase(this);
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
    presenter.requestClearAll(this);
    return true;
  }

  @Override protected boolean onLicenseItemClicked() {
    MainActivity.getNavigationDrawerController(getActivity()).drawerShowUpNavigation();
    return super.onLicenseItemClicked();
  }

  @Override public void showConfirmDialog(int type) {
    AppUtil.onlyLoadOnceDialogFragment(getActivity(), ConfirmationDialog.newInstance(type),
        "confirm_dialog");
  }

  @Override public void onStart() {
    super.onStart();
    presenter.bindView(null);
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
