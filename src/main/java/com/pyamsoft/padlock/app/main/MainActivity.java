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

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.accessibility.AccessibilityFragment;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.pydroid.base.activity.DonationActivityBase;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.StringUtil;
import timber.log.Timber;

public class MainActivity extends DonationActivityBase
    implements MainPresenter.MainView, RatingDialog.ChangeLogProvider {

  @BindView(R.id.main_root) CoordinatorLayout rootView;
  @BindView(R.id.toolbar) Toolbar toolbar;
  MainPresenter presenter;
  private Unbinder unbinder;

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    unbinder = ButterKnife.bind(this);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    getSupportLoaderManager().initLoader(0, null,
        new LoaderManager.LoaderCallbacks<MainPresenter>() {
          @Override public Loader<MainPresenter> onCreateLoader(int id, Bundle args) {
            return new MainPresenterLoader(getApplicationContext());
          }

          @Override public void onLoadFinished(Loader<MainPresenter> loader, MainPresenter data) {
            presenter = data;
          }

          @Override public void onLoaderReset(Loader<MainPresenter> loader) {
            presenter = null;
          }
        });

    setAppBarState();
  }

  @Override protected int bindActivityToView() {
    setContentView(R.layout.activity_main);
    return R.id.ad_view;
  }

  @Override protected void onStart() {
    super.onStart();
    presenter.bindView(this);
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  private void setAppBarState() {
    setSupportActionBar(toolbar);
    toolbar.setTitle(getString(R.string.app_name));
  }

  private void showAccessibilityPrompt() {
    supportInvalidateOptionsMenu();
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(AccessibilityFragment.TAG) == null) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, new AccessibilityFragment(), AccessibilityFragment.TAG)
          .commit();
    }
  }

  private void showLockList() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(LockListFragment.TAG) == null
        && fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null) {
      rootView.getViewTreeObserver()
          .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
              rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

              supportInvalidateOptionsMenu();
              final View decorView = getWindow().getDecorView();
              final int cX = decorView.getLeft() + decorView.getWidth() / 2;
              final int cY = decorView.getBottom() + decorView.getHeight() / 2;
              fragmentManager.beginTransaction()
                  .replace(R.id.main_view_container, LockListFragment.newInstance(cX, cY),
                      LockListFragment.TAG)
                  .commit();
            }
          });
    }
  }

  @CheckResult @NonNull public View getSettingsMenuItemView() {
    final View amv = toolbar.getChildAt(1);
    if (amv != null && amv instanceof ActionMenuView) {
      final ActionMenuView actions = (ActionMenuView) amv;
      // Settings gear is the second item
      return actions.getChildAt(1);
    } else {
      throw new RuntimeException("Could not locate view for Settings menu item");
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    unbinder.unbind();
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
  }

  @Override public void onBackPressed() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    if (backStackCount > 0) {
      fragmentManager.popBackStack();
    } else {
      super.onBackPressed();
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
    finish();
  }

  @Override protected void onPostResume() {
    super.onPostResume();

    AnimUtil.animateActionBarToolbar(toolbar);
    RatingDialog.showRatingDialog(this, this);
    if (PadLockService.isRunning()) {
      showLockList();
      presenter.showTermsDialog();
    } else {
      showAccessibilityPrompt();
    }
  }

  @Override public void showUsageTermsDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new AgreeTermsDialog(),
        AgreeTermsDialog.TAG);
  }

  @NonNull @Override public Spannable getChangeLogText() {
    // The changelog text
    final String title = "What's New in Version " + BuildConfig.VERSION_NAME;
    final String line1 = "BUGFIX: Fixed crashes on devices below Lollipop";
    final String line2 = "BUGFIX: Faster loading and properly disposing of icons";
    final String line3 =
        "FEATURE: Periodically re-check the active application and lock it if necessary";

    // Turn it into a spannable
    final Spannable spannable = StringUtil.createLineBreakBuilder(title, line1, line2, line3);

    int start = 0;
    int end = title.length();
    final int largeSize =
        StringUtil.getTextSizeFromAppearance(this, android.R.attr.textAppearanceLarge);
    final int largeColor =
        StringUtil.getTextColorFromAppearance(this, android.R.attr.textAppearanceLarge);
    final int smallSize =
        StringUtil.getTextSizeFromAppearance(this, android.R.attr.textAppearanceSmall);
    final int smallColor =
        StringUtil.getTextColorFromAppearance(this, android.R.attr.textAppearanceSmall);

    StringUtil.boldSpan(spannable, start, end);
    StringUtil.sizeSpan(spannable, start, end, largeSize);
    StringUtil.colorSpan(spannable, start, end, largeColor);

    start += end + 2;
    end += 2 + line1.length() + 2 + line2.length() + 2 + line3.length();

    StringUtil.sizeSpan(spannable, start, end, smallSize);
    StringUtil.colorSpan(spannable, start, end, smallColor);

    return spannable;
  }

  @Override public int getChangeLogIcon() {
    return R.mipmap.ic_launcher;
  }

  @NonNull @Override public String getChangeLogPackageName() {
    return getPackageName();
  }

  @Override public int getChangeLogVersion() {
    return BuildConfig.VERSION_CODE;
  }

  @Override public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    Timber.d("onCreateOptionsMenu");
    return super.onCreateOptionsMenu(menu);
  }

  @Override public void forceRefresh() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    showLockList();
  }
}

