/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.main;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentMainBinding;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.purge.PurgeFragment;
import com.pyamsoft.padlock.settings.SettingsFragment;
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarFragment;
import timber.log.Timber;

public class MainFragment extends ActionBarFragment {

  @NonNull public static final String TAG = "MainFragment";
  @SuppressWarnings("WeakerAccess") FragmentMainBinding binding;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentMainBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupBottomNavigation();

    if (getChildFragmentManager().findFragmentById(R.id.main_view_container) == null) {
      Timber.d("Load default Tab: List");
      binding.bottomTabs.getMenu().performIdentifierAction(R.id.menu_locklist, 0);
    }
  }

  private void setupBottomNavigation() {
    binding.bottomTabs.setOnNavigationItemSelectedListener(
        new BottomNavigationView.OnNavigationItemSelectedListener() {
          @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final boolean handled;
            switch (item.getItemId()) {
              case R.id.menu_locklist:
                handled = replaceFragment(new LockListFragment(), LockListFragment.TAG);
                break;
              case R.id.menu_settings:
                handled = replaceFragment(new SettingsFragment(), SettingsFragment.TAG);
                break;
              case R.id.menu_purge:
                handled = replaceFragment(new PurgeFragment(), PurgeFragment.TAG);
                break;
              default:
                handled = false;
            }

            if (handled) {
              item.setChecked(!item.isChecked());
            }

            return handled;
          }

          @CheckResult
          private boolean replaceFragment(@NonNull Fragment fragment, @NonNull String tag) {
            final FragmentManager fragmentManager = getChildFragmentManager();
            if (fragmentManager.findFragmentByTag(tag) == null) {
              fragmentManager.beginTransaction()
                  .replace(R.id.main_view_container, fragment, tag)
                  .commit();
              return true;
            } else {
              return false;
            }
          }
        });
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(false);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    binding.unbind();
  }
}

