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

import android.app.Activity;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.ActivityMainBinding;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.onboard.firstlaunch.OnboardFragment;
import com.pyamsoft.padlock.purge.PurgeFragment;
import com.pyamsoft.padlock.settings.SettingsFragment;
import com.pyamsoft.pydroid.ads.AdSource;
import com.pyamsoft.pydroid.ui.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.ui.ads.OnlineAdSource;
import com.pyamsoft.pydroid.ui.sec.TamperActivity;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity extends TamperActivity
    implements NavigationDrawerController, MainPresenter.OnboardingCallback {

  @NonNull private static final String FIRST_LAUNCH = "main_first_launch";
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @SuppressWarnings("WeakerAccess") @Inject MainPresenter presenter;
  @SuppressWarnings("WeakerAccess") ActivityMainBinding binding;
  @SuppressWarnings("WeakerAccess") boolean firstLaunch;
  private ActionBarDrawerToggle drawerToggle;

  @CheckResult @NonNull public static NavigationDrawerController getNavigationDrawerController(
      @NonNull Activity activity) {
    if (activity instanceof NavigationDrawerController) {
      return (NavigationDrawerController) activity;
    } else {
      throw new IllegalStateException("Activity is not Drawer Controller");
    }
  }

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    firstLaunch = savedInstanceState != null && savedInstanceState.getBoolean(FIRST_LAUNCH, false);

    Injector.get().provideComponent().plusMainComponent().inject(this);

    setAppBarState();
    setupDrawerLayout();
  }

  private void setupDrawerLayout() {
    drawerToggle =
        new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.blank,
            R.string.blank);

    drawerToggle.setToolbarNavigationClickListener(v -> onBackPressed());

    binding.navigationDrawer.setNavigationItemSelectedListener(item -> {
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
        binding.drawerLayout.closeDrawer(binding.navigationDrawer);
      }

      return toggleChecked;
    });
    binding.drawerLayout.addDrawerListener(drawerToggle);
  }

  void peekNavigationDrawer() {
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN,
        binding.navigationDrawer);

    handler.postDelayed(() -> {
      binding.drawerLayout.closeDrawer(binding.navigationDrawer);
      drawerNormalNavigation();
    }, 1200L);
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
      drawerShowUpNavigation();
      changed = FragmentHasChanged.CHANGED_WITH_UP;
      // These are base fragments
    } else if (fragmentManager.findFragmentByTag(LockListFragment.TAG) == null
        && fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null
        && fragmentManager.findFragmentByTag(OnboardFragment.TAG) == null
        && fragmentManager.findFragmentByTag(PurgeFragment.TAG) == null) {
      binding.navigationDrawer.getMenu().performIdentifierAction(R.id.menu_locklist, 0);
      changed = FragmentHasChanged.CHANGED_NO_UP;
    } else {
      if (fragmentManager.findFragmentByTag(OnboardFragment.TAG) != null) {
        prepareActivityForOnboarding();
      }
      changed = FragmentHasChanged.NOT_CHANGED;
    }

    return changed;
  }

  @Override public void drawerNormalNavigation() {
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED,
        binding.navigationDrawer);
    drawerToggle.setDrawerIndicatorEnabled(true);
    drawerToggle.syncState();
  }

  @Override public void drawerShowUpNavigation() {
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
        binding.navigationDrawer);
    drawerToggle.setDrawerIndicatorEnabled(false);
    drawerToggle.syncState();
  }

  @Override protected int bindActivityToView() {
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    return R.id.ad_view;
  }

  @Override protected void onStart() {
    super.onStart();
    presenter.showOnboardingOrDefault(this);
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
    handler.removeCallbacksAndMessages(null);
    binding.drawerLayout.removeDrawerListener(drawerToggle);
    binding.unbind();
  }

  @Override public void onBackPressed() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    if (backStackCount > 0) {
      fragmentManager.popBackStackImmediate();
      if (backStackCount == 1) {
        drawerNormalNavigation();
      }
    } else {
      super.onBackPressed();
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(FIRST_LAUNCH, firstLaunch);
    super.onSaveInstanceState(outState);
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

  @Override protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    drawerToggle.syncState();
  }

  @Override public boolean onOptionsItemSelected(final @NonNull MenuItem item) {
    if (drawerToggle.onOptionsItemSelected(item)) {
      Timber.d("Handled by drawer toggle");
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    AnimUtil.animateActionBarToolbar(binding.toolbar);
  }

  @NonNull @Override protected String getSafePackageName() {
    return "com.pyamsoft.padlock";
  }

  @NonNull @Override protected String[] getChangeLogLines() {
    final String line1 = "BUGFIX: Cache loaded lists for quicker performance";
    final String line2 = "BUGFIX: Shrink APK size";
    final String line3 = "BUGFIX: Less memory usage";
    return new String[] { line1, line2, line3 };
  }

  @NonNull @Override protected String getVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  @Override public int getApplicationIcon() {
    return R.mipmap.ic_launcher;
  }

  /**
   * Hide action bar, lock drawer
   */
  void prepareActivityForOnboarding() {
    // Hide the action bar
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      if (actionBar.isShowing()) {
        actionBar.hide();
      }
    }

    // Lock the navigation drawer if we are showing onboarding
    // Action bar is hidden so the drawer toggle state wont matter
    drawerShowUpNavigation();
  }

  public void onOnboardingCompleted() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final Fragment onboarding = fragmentManager.findFragmentByTag(OnboardFragment.TAG);
    if (onboarding != null) {
      fragmentManager.beginTransaction().remove(onboarding).commitNow();
    }

    presenter.showOnboardingOrDefault(this);
  }

  @Override public void onShowOnboarding() {
    if (replaceFragment(new OnboardFragment(), OnboardFragment.TAG)) {
      Timber.d("New onboarding fragment placed");
    }

    prepareActivityForOnboarding();
  }

  @Override public void onShowDefaultPage() {
    // Set normal navigation
    final FragmentHasChanged changed = loadFragment();
    if (changed == FragmentHasChanged.NOT_CHANGED) {
      Timber.d("Fragment has not changed");
    } else {
      // Un hide the action bar in case it was hidden
      final ActionBar actionBar = getSupportActionBar();
      if (actionBar != null) {
        if (!actionBar.isShowing()) {
          actionBar.show();
        }
      }
      if (changed == FragmentHasChanged.CHANGED_WITH_UP) {
        drawerShowUpNavigation();
      } else {
        drawerNormalNavigation();
      }

      if (firstLaunch) {
        peekNavigationDrawer();
      }
    }
  }

  @Nullable @Override protected AdSource provideOnlineAdSource() {
    OnlineAdSource source = new OnlineAdSource(R.string.banner_main_ad_id);
    source.addTestAdIds("E1241303FDC266381AD6C9FF6FAD056B");
    return source;
  }

  private enum FragmentHasChanged {
    NOT_CHANGED, CHANGED_WITH_UP, CHANGED_NO_UP,
  }
}

