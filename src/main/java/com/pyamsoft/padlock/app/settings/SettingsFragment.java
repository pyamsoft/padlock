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

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.base.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.base.fragment.CircularRevealFragmentUtil;

public class SettingsFragment extends ActionBarFragment {

  @NonNull public static final String TAG = "SettingsFragment";

  @CheckResult @NonNull
  public static Fragment newInstance(@NonNull View fromView, @NonNull View containerView) {
    final Fragment fragment = new SettingsFragment();
    fragment.setArguments(CircularRevealFragmentUtil.bundleArguments(fromView, containerView, 0));
    return fragment;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_settings, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    CircularRevealFragmentUtil.runCircularRevealOnViewCreated(view, getArguments());
    displayPreferenceFragment();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(true);
  }

  // KLUDGE child fragment, not the nicest
  private void displayPreferenceFragment() {
    if (getChildFragmentManager().findFragmentByTag(SettingsPreferenceFragment.TAG) == null) {
      getChildFragmentManager().beginTransaction()
          .add(R.id.settings_preferences_container, new SettingsPreferenceFragment(),
              SettingsPreferenceFragment.TAG)
          .commit();
    }
  }
}
