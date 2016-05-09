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

import android.animation.LayoutTransition;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.dagger.main.DaggerMainComponent;
import com.pyamsoft.padlock.dagger.main.MainModule;
import com.pyamsoft.pydroid.base.ActivityBase;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.DrawableUtil;
import java.lang.ref.WeakReference;
import javax.inject.Inject;

public class MainActivity extends ActivityBase implements MainView {

  private static final String USAGE_TERMS_TAG = "usage_terms";
  private static final int VECTOR_TASK_SIZE = 2;

  @NonNull private final AsyncVectorDrawableTask[] tasks =
      new AsyncVectorDrawableTask[VECTOR_TASK_SIZE];
  @NonNull private final LockListFragment lockListFragment;
  @NonNull private final SettingsFragment settingsFragment;
  @BindView(R.id.main_view) CoordinatorLayout mainView;
  @BindView(R.id.main_pager) ViewPager viewPager;
  @BindView(R.id.main_enable_service) LinearLayout enableService;
  @BindView(R.id.main_service_button) Button serviceButton;
  @BindView(R.id.main_tabs) TabLayout tabLayout;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbar) AppBarLayout appBarLayout;
  @Inject MainPresenter presenter;
  private MainPagerAdapter adapter;
  private Unbinder unbinder;

  @SuppressWarnings("WeakerAccess") public MainActivity() {
    lockListFragment = new LockListFragment();
    settingsFragment = new SettingsFragment();
  }

  @Override public void onCreate(final Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    unbinder = ButterKnife.bind(this);

    DaggerMainComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .mainModule(new MainModule())
        .build()
        .inject(this);

    presenter.start(this);

    setAppBarState();
    setupViewPagerAdapter();
    setupViewPager();
    setupTabLayout();
    setupAccessibilityButton();
  }

  private void setupAccessibilityButton() {
    serviceButton.setOnClickListener(view -> {
      final Intent accessibilityServiceIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      startActivity(accessibilityServiceIntent);
    });
  }

  private void showAccessibilityPrompt() {
    supportInvalidateOptionsMenu();
    viewPager.setVisibility(View.GONE);
    tabLayout.setVisibility(View.GONE);
    enableService.setVisibility(View.VISIBLE);
  }

  private void showViewPager() {
    supportInvalidateOptionsMenu();
    enableService.setVisibility(View.GONE);
    viewPager.setVisibility(View.VISIBLE);
    tabLayout.setVisibility(View.VISIBLE);
  }

  private void setupViewPagerAdapter() {
    adapter = new MainPagerAdapter(getSupportFragmentManager());
    adapter.setLockListFragment(lockListFragment);
    adapter.setSettingsFragment(settingsFragment);
  }

  private void cancelAsyncVectorTask(int position) {
    if (tasks[position] != null) {
      if (!tasks[position].isCancelled()) {
        tasks[position].cancel(true);
      }
      tasks[position] = null;
    }
  }

  private void setupTabLayout() {
    tabLayout.setupWithViewPager(viewPager);
    tabLayout.setTabMode(TabLayout.MODE_FIXED);
    tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    final int size = tabLayout.getTabCount();
    for (int i = 0; i < size; ++i) {
      final TabLayout.Tab tab = tabLayout.getTabAt(i);
      if (tab != null) {
        final int color = (i == 0) ? android.R.color.white : R.color.grey500;
        int ic;
        switch (i) {
          case MainPagerAdapter.LIST:
            ic = R.drawable.ic_check_24dp;
            break;
          case MainPagerAdapter.SETTINGS:
            ic = R.drawable.ic_settings_24dp;
            break;
          default:
            ic = 0;
        }
        if (ic != 0) {
          cancelAsyncVectorTask(i);
          tasks[i] = new AsyncVectorDrawableTask(tab, color);
          tasks[i].execute(new AsyncDrawable(getApplicationContext(), ic));
        }
      }
    }
    tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

      private final WeakReference<MainActivity> weakActivity =
          new WeakReference<>(MainActivity.this);

      @Override public void onTabSelected(TabLayout.Tab tab) {
        final MainActivity activity = weakActivity.get();
        if (activity != null) {
          tab.setIcon(
              DrawableUtil.tintDrawableFromRes(activity, tab.getIcon(), android.R.color.white));
        }
        if (viewPager != null) {
          viewPager.setCurrentItem(tab.getPosition());
        }
      }

      @Override public void onTabUnselected(TabLayout.Tab tab) {
        final MainActivity activity = weakActivity.get();
        if (activity != null) {
          tab.setIcon(DrawableUtil.tintDrawableFromRes(activity, tab.getIcon(), R.color.grey500));
        }
      }

      @Override public void onTabReselected(TabLayout.Tab tab) {

      }
    });
  }

  private void setupViewPager() {
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
        final int size = adapter.getCount();
        for (int i = 0; i < size; ++i) {
          if (i == position) {
            adapter.onPageSelected(viewPager, i);
          } else {
            adapter.onPageUnselected(viewPager, i);
          }
        }
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });
    viewPager.setAdapter(adapter);
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    if (tabLayout != null) {
      tabLayout.setOnTabSelectedListener(null);
    }

    for (int i = 0; i < VECTOR_TASK_SIZE; ++i) {
      cancelAsyncVectorTask(i);
    }

    presenter.stop();
    if (!isChangingConfigurations()) {
      presenter.destroy();
    }

    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  private void setAppBarState() {
    setSupportActionBar(toolbar);
    toolbar.setTitle(getString(R.string.app_name));
    setActionBarUpEnabled(false);

    // Enable animations on Change on the root layout
    final LayoutTransition rootLayoutTransition = mainView.getLayoutTransition();
    if (rootLayoutTransition != null) {
      rootLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
  }

  @Override public boolean onOptionsItemSelected(final MenuItem item) {
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

  @Override public String getPlayStoreAppPackage() {
    return getPackageName();
  }

  @Override protected void onResume() {
    super.onResume();
    animateActionBarToolbar(toolbar);

    presenter.registerOnConfirmDialogBus();
    presenter.registerOnAgreeTermsBus();
  }

  @Override public void onDidNotAgreeToTerms() {
    finish();
  }

  @Override protected void onPause() {
    super.onPause();

    presenter.unregisterFromConfirmDialogBus();
    presenter.unregisterFromAgreeTermsBus();
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    if (!BillingProcessor.isIabServiceAvailable(this)) {
      showDonationUnavailableDialog();
    }

    if (PadLockService.isEnabled()) {
      showViewPager();
      presenter.showTermsDialog();
    } else {
      showAccessibilityPrompt();
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    if (isFinishing()) {
      DataHolderFragment.removeAll(this);
    }

    super.onSaveInstanceState(outState);
  }

  @Override public void showUsageTermsDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new AgreeTermsDialog(),
        USAGE_TERMS_TAG);
  }
}

