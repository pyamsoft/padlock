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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.ActivityMainBinding;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardFragment;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.sec.TamperActivity;
import com.pyamsoft.pydroid.ui.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity extends TamperActivity {

  @SuppressWarnings("WeakerAccess") @Inject MainPresenter presenter;
  @SuppressWarnings("WeakerAccess") ActivityMainBinding binding;

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    Injector.get().provideComponent().inject(this);

    setAppBarState();
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
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(OnboardFragment.TAG) == null) {
          fm.beginTransaction()
              .replace(R.id.fragment_container, new OnboardFragment(), OnboardFragment.TAG)
              .commitNow();
        }
        prepareActivityForOnboarding();
      }

      @Override public void onShowDefaultPage() {
        // Set normal navigation
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(OnboardFragment.TAG) != null) {
          Timber.w(
              "Show default page was called, but Onboarding fragment still exists, continue onboarding");
          prepareActivityForOnboarding();
        } else {
          // Un hide the action bar in case it was hidden
          final ActionBar actionBar = getSupportActionBar();
          if (actionBar != null) {
            if (!actionBar.isShowing()) {
              actionBar.show();
            }
          }

          if (fm.findFragmentByTag(MainFragment.TAG) == null
              && fm.findFragmentByTag(AboutLibrariesFragment.TAG) == null) {
            Timber.d("Load default page");
            fm.beginTransaction()
                .replace(R.id.fragment_container, new MainFragment(), MainFragment.TAG)
                .commitNow();
          } else {
            Timber.w("Default page or About libraries was already loaded");
          }
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
  }
}

