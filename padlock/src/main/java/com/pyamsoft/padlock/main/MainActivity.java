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

package com.pyamsoft.padlock.main;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.ActivityMainBinding;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardFragment;
import com.pyamsoft.padlock.purge.PurgeFragment;
import com.pyamsoft.padlock.settings.SettingsFragment;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.sec.TamperActivity;
import com.pyamsoft.pydroid.ui.util.ActionBarUtil;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity extends TamperActivity {

  @NonNull private static final String FIRST_LAUNCH = "main_first_launch";
  @SuppressWarnings("WeakerAccess") @Inject MainPresenter presenter;
  @SuppressWarnings("WeakerAccess") ActivityMainBinding binding;

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    Injector.get().provideComponent().plusMainComponent().inject(this);

    setAppBarState();
    setupBottomNavigation();
  }

  private void setupBottomNavigation() {
    binding.bottomTabs.setOnNavigationItemSelectedListener(item -> {
      final boolean toggleChecked;
      switch (item.getItemId()) {
        case R.id.menu_locklist:
          toggleChecked = replaceFragment(new LockListFragment(), LockListFragment.TAG);
          break;
        case R.id.menu_settings:
          toggleChecked = replaceFragment(new SettingsFragment(), SettingsFragment.TAG);
          break;
        case R.id.menu_purge:
          toggleChecked = replaceFragment(new PurgeFragment(), PurgeFragment.TAG);
          break;
        default:
          toggleChecked = false;
      }

      if (toggleChecked) {
        item.setChecked(!item.isChecked());
      }

      return toggleChecked;
    });
  }

  @SuppressWarnings("WeakerAccess") @CheckResult boolean replaceFragment(@NonNull Fragment fragment,
      @NonNull String tag) {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(tag) == null) {
      fragmentManager.beginTransaction().replace(R.id.main_view_container, fragment, tag).commit();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns if the fragment has changed
   */
  @CheckResult @NonNull FragmentHasChanged loadFragment() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final FragmentHasChanged changed;

    // These fragments are a level up
    if (fragmentManager.findFragmentByTag(AboutLibrariesFragment.TAG) != null) {
      changed = FragmentHasChanged.CHANGED_WITH_UP;
      // These are base fragments
    } else if (fragmentManager.findFragmentByTag(LockListFragment.TAG) == null
        && fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null
        && fragmentManager.findFragmentByTag(OnboardFragment.TAG) == null
        && fragmentManager.findFragmentByTag(PurgeFragment.TAG) == null) {
      changed = FragmentHasChanged.CHANGED_NO_UP;
    } else {
      changed = FragmentHasChanged.NOT_CHANGED;
    }

    return changed;
  }

  @Override protected void onStart() {
    super.onStart();
    showOnboardingOrDefault();
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.stop();
  }

  @Override protected void onPause() {
    super.onPause();
    if (isFinishing() || isChangingConfigurations()) {
      Timber.d(
          "Even though a leak is reported, this should dismiss the window, and clear the leak");
      binding.toolbar.getMenu().close();
      binding.toolbar.dismissPopupMenus();
    }
  }

  @NonNull @Override public String provideApplicationName() {
    return "PadLock";
  }

  @Override public int getCurrentApplicationVersion() {
    return BuildConfig.VERSION_CODE;
  }

  private void setAppBarState() {
    setSupportActionBar(binding.toolbar);
    binding.toolbar.setTitle(getString(R.string.app_name));
    ViewCompat.setElevation(binding.toolbar, AppUtil.convertToDP(this, 4));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    binding.unbind();
  }

  @Override public void onBackPressed() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    if (backStackCount > 0) {
      fragmentManager.popBackStackImmediate();
    } else {
      super.onBackPressed();
    }
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    AnimUtil.animateActionBarToolbar(binding.toolbar);
  }

  @NonNull @Override protected String getSafePackageName() {
    return "com.pyamsoft.padlock";
  }

  @NonNull @Override protected String[] getChangeLogLines() {
    final String line1 = "BUGFIX: Bugfixes and improvements";
    final String line2 = "BUGFIX: Removed all Advertisements";
    final String line3 = "BUGFIX: Faster loading of Open Source Licenses page";
    return new String[] { line1, line2, line3 };
  }

  @NonNull @Override protected String getVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  @Override public int getApplicationIcon() {
    return R.mipmap.ic_launcher;
  }

  /**
   * Called from Onboarding fragments
   */
  public void onOnboardingCompleted() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final Fragment onboarding = fragmentManager.findFragmentByTag(OnboardFragment.TAG);
    if (onboarding != null) {
      fragmentManager.beginTransaction().remove(onboarding).commitNow();
    }

    showOnboardingOrDefault();
  }

  private void showOnboardingOrDefault() {
    presenter.showOnboardingOrDefault(new MainPresenter.OnboardingCallback() {
      @Override public void onShowOnboarding() {
        if (replaceFragment(new OnboardFragment(), OnboardFragment.TAG)) {
          Timber.d("New onboarding fragment placed");
        }

        prepareActivityForOnboarding();
      }

      @Override public void onShowDefaultPage() {
        // Set normal navigation
        final boolean showBottomBar;
        final FragmentHasChanged changed = loadFragment();
        if (changed == FragmentHasChanged.NOT_CHANGED) {
          Timber.d("Fragment has not changed");
          if (getSupportFragmentManager().findFragmentByTag(OnboardFragment.TAG) != null) {
            prepareActivityForOnboarding();
            showBottomBar = false;
          } else {
            showBottomBar = true;
          }
        } else {
          // Un hide the action bar in case it was hidden
          final ActionBar actionBar = getSupportActionBar();
          if (actionBar != null) {
            if (!actionBar.isShowing()) {
              actionBar.show();
            }
          }
          if (changed == FragmentHasChanged.CHANGED_WITH_UP) {
            ActionBarUtil.setActionBarUpEnabled(MainActivity.this, true);
          } else {
            ActionBarUtil.setActionBarUpEnabled(MainActivity.this, false);
            binding.bottomTabs.getMenu().performIdentifierAction(R.id.menu_locklist, 0);
          }

          showBottomBar = true;
        }

        if (showBottomBar) {
          binding.bottomTabs.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  void prepareActivityForOnboarding() {
    // Hide the action bar
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      if (actionBar.isShowing()) {
        actionBar.hide();
      }
    }

    // Hide bottom bar
    binding.bottomTabs.setVisibility(View.GONE);
  }

  private enum FragmentHasChanged {
    NOT_CHANGED, CHANGED_WITH_UP, CHANGED_NO_UP,
  }
}

