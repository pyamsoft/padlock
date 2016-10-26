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

package com.pyamsoft.padlock.app.main;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ActionMenuView;
import android.view.MenuItem;
import android.view.View;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.LockInfoFragment;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.purge.PurgeFragment;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.databinding.ActivityMainBinding;
import com.pyamsoft.pydroid.about.AboutLibrariesFragment;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.support.RatingActivity;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import timber.log.Timber;

public class MainActivity extends RatingActivity implements MainPresenter.MainView {

  @NonNull private static final String KEY_PRESENTER = "key_main_presenter";
  @SuppressWarnings("WeakerAccess") MainPresenter presenter;
  private ActivityMainBinding binding;
  private long loaderKey;

  // KLUDGE When the Onboarding TapTargetView is shown, pressing the back button can result in crashing
  // KLUDGE thus, we disable the back button while target is shown
  private boolean backButtonEnabled = true;

  public void setBackButtonEnabled(boolean backButtonEnabled) {
    this.backButtonEnabled = backButtonEnabled;
  }

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    setBackButtonEnabled(true);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    loaderKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<MainPresenter>() {
          @NonNull @Override public PersistLoader<MainPresenter> createLoader() {
            return new MainPresenterLoader();
          }

          @Override public void onPersistentLoaded(@NonNull MainPresenter persist) {
            presenter = persist;
          }
        });

    setAppBarState();
  }

  @CheckResult @NonNull MainPresenter getPresenter() {
    if (presenter == null) {
      throw new NullPointerException("MainPresenter is NULL");
    }

    return presenter;
  }

  @Override protected int bindActivityToView() {
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    return R.id.ad_view;
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loaderKey);
    super.onSaveInstanceState(outState);
  }

  @Override protected void onStart() {
    super.onStart();
    presenter.bindView(this);
    showLockList(false);
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.unbindView();
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
  }

  private void showLockList(boolean forceRefresh) {
    supportInvalidateOptionsMenu();
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if ((fragmentManager.findFragmentByTag(LockListFragment.TAG) == null
        && fragmentManager.findFragmentByTag(LockInfoFragment.TAG) == null
        && fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null
        && fragmentManager.findFragmentByTag(PurgeFragment.TAG) == null
        && fragmentManager.findFragmentByTag(AboutLibrariesFragment.TAG) == null) || forceRefresh) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, LockListFragment.newInstance(forceRefresh),
              LockListFragment.TAG)
          .commitNow();
    }
  }

  @CheckResult @NonNull public View getSettingsMenuItemView() {
    final View amv = binding.toolbar.getChildAt(1);
    if (amv instanceof ActionMenuView) {
      final ActionMenuView actions = (ActionMenuView) amv;
      // Settings gear is the second item
      return actions.getChildAt(1);
    } else {
      throw new RuntimeException("Could not locate view for Settings menu item");
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (!isChangingConfigurations()) {
      PersistentCache.get().unload(loaderKey);
    }
    binding.unbind();
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
  }

  @Override public void onBackPressed() {
    if (backButtonEnabled) {
      final FragmentManager fragmentManager = getSupportFragmentManager();
      final int backStackCount = fragmentManager.getBackStackEntryCount();
      if (backStackCount > 0) {
        fragmentManager.popBackStack();
      } else {
        super.onBackPressed();
      }
    } else {
      Timber.w("Back button action is disabled due to onboarding");
    }
  }

  @Override public boolean onOptionsItemSelected(final @NonNull MenuItem item) {
    final int itemId = item.getItemId();
    boolean handled;
    switch (itemId) {
      case android.R.id.home:
        onBackPressed();
        handled = true;
        break;
      default:
        handled = false;
    }
    return handled || super.onOptionsItemSelected(item);
  }

  @Override public void onDidNotAgreeToTerms() {
    Timber.e("Did not agree to terms");
    finish();
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    AnimUtil.animateActionBarToolbar(binding.toolbar);
    RatingDialog.showRatingDialog(this, this);
    presenter.showTermsDialog();
  }

  @Override public void showUsageTermsDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new AgreeTermsDialog(),
        AgreeTermsDialog.TAG);
  }

  @NonNull @Override protected String[] getChangeLogLines() {
    final String line1 = "FEATURE: Faster application list loading on Android N";
    final String line2 =
        "FEATURE: When a new application is installed, suggest to lock it with PadLock";
    final String line3 = "BUGFIX: Better locking in Multiwindow and Freeform mode on Android N";
    return new String[] { line1, line2, line3 };
  }

  @NonNull @Override protected String getVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  @Override public int getApplicationIcon() {
    return R.mipmap.ic_launcher;
  }

  @Override public void forceRefresh() {
    Timber.d("Force lock list refresh");
    final FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    showLockList(true);
  }
}

