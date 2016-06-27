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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.accessibility.AccessibilityFragment;
import com.pyamsoft.padlock.app.list.LockListFragment;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.dagger.main.DaggerMainComponent;
import com.pyamsoft.padlock.dagger.main.MainModule;
import com.pyamsoft.pydroid.base.activity.DonationActivityBase;
import com.pyamsoft.pydroid.support.RatingDialog;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.StringUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class MainActivity extends DonationActivityBase
    implements MainPresenter.MainView, RatingDialog.ChangeLogProvider {

  @NonNull public static final String SETTINGS_TAG = "settings";
  @NonNull public static final String USAGE_TERMS_TAG = "usage_terms";
  @NonNull public static final String ACCESSIBILITY_TAG = "accessibility";
  @NonNull public static final String LOCK_LIST_TAG = "lock_list";
  private static final int VECTOR_TASK_SIZE = 2;

  @NonNull private final AsyncVectorDrawableTask[] tasks;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @Inject MainPresenter presenter;
  private Unbinder unbinder;

  public MainActivity() {
    tasks = new AsyncVectorDrawableTask[VECTOR_TASK_SIZE];
  }

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
    unbinder = ButterKnife.bind(this);

    DaggerMainComponent.builder()
        .padLockComponent(PadLock.getInstance().getPadLockComponent())
        .mainModule(new MainModule())
        .build()
        .inject(this);

    presenter.bindView(this);

    setAppBarState();
  }

  private void showAccessibilityPrompt() {
    supportInvalidateOptionsMenu();
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(ACCESSIBILITY_TAG) == null) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, new AccessibilityFragment(), ACCESSIBILITY_TAG)
          .commit();
    }
  }

  private void showLockList() {
    supportInvalidateOptionsMenu();
    final FragmentManager fragmentManager = getSupportFragmentManager();
    if (fragmentManager.findFragmentByTag(LOCK_LIST_TAG) == null
        && fragmentManager.findFragmentByTag(SETTINGS_TAG) == null) {
      fragmentManager.beginTransaction()
          .replace(R.id.main_view_container, new LockListFragment(), LOCK_LIST_TAG)
          .commit();
    }
  }

  private void cancelAsyncVectorTask(int position) {
    if (tasks[position] != null) {
      if (!tasks[position].isCancelled()) {
        tasks[position].cancel(true);
      }
      tasks[position] = null;
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    if (!isChangingConfigurations()) {
      presenter.unbindView();
    }

    unbinder.unbind();

    for (int i = 0; i < VECTOR_TASK_SIZE; ++i) {
      cancelAsyncVectorTask(i);
    }
  }

  private void setAppBarState() {
    setSupportActionBar(toolbar);
    toolbar.setTitle(getString(R.string.app_name));
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    setActionBarUpEnabled(backStackCount > 0);
  }

  @Override protected boolean shouldConfirmBackPress() {
    return true;
  }

  @Override public void onBackPressed() {
    final FragmentManager fragmentManager = getSupportFragmentManager();
    final int backStackCount = fragmentManager.getBackStackEntryCount();
    if (backStackCount > 0) {
      fragmentManager.popBackStack();
      if (backStackCount - 1 == 0) {
        setActionBarUpEnabled(false);
      }
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

  @NonNull @Override public String getPlayStoreAppPackage() {
    return getPackageName();
  }

  @Override protected void onResume() {
    super.onResume();
    animateActionBarToolbar(toolbar);

    presenter.onResume();
  }

  @Override public void onDidNotAgreeToTerms() {
    finish();
  }

  @Override protected void onPause() {
    super.onPause();
    presenter.onPause();
  }

  @Override protected void onPostResume() {
    super.onPostResume();

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
        USAGE_TERMS_TAG);
  }

  @NonNull @Override public Spannable getChangeLogText() {
    // The changelog text
    final String title = "What's New in Version " + BuildConfig.VERSION_NAME;
    final String line1 =
        "BUGFIX: Fixed issue where the Lock Info Dialog would not display after navigating to the Settings screen";
    final String line2 =
        "BUGFIX: Fixed issue in Android N which prevented loading the list of applications";
    final String line3 = "FEATURE: Smaller memory footprint when the list is being populated";
    final String line4 = "BUGFIX: The LockScreen is now less intrusive.";
    final String line5 =
        "FEATURE: Locking an application using the switch on the main list locks the entire 'package'. "
            + "This means that unlocking one screen of the application unlocks all screens. "
            + "The only exception to this are the screens that the user chooses to individually "
            + "lock by checking the box in the Info Dialog.";

    // Turn it into a spannable
    final Spannable spannable =
        StringUtil.createLineBreakBuilder(title, line1, line2, line3, line4, line5);

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
    end += 2
        + line1.length()
        + 2
        + line2.length()
        + 2
        + line3.length()
        + 2
        + line4.length()
        + 2
        + line5.length();

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
    setActionBarUpEnabled(false);
    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    fragmentManager.beginTransaction()
        .replace(R.id.main_view_container, new LockListFragment(), LOCK_LIST_TAG)
        .commit();
  }
}

