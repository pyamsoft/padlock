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

package com.pyamsoft.padlock.app.lock;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.dagger.lock.DaggerLockScreenComponent;
import com.pyamsoft.padlock.dagger.lock.LockScreenModule;
import com.pyamsoft.pydroid.base.NoDonationActivityBase;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public final class LockScreenActivity extends NoDonationActivityBase implements LockScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = LockViewDelegate.ENTRY_PACKAGE_NAME;
  @NonNull public static final String ENTRY_ACTIVITY_NAME = LockViewDelegate.ENTRY_ACTIVITY_NAME;
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";

  @NonNull private final Intent home;
  @Nullable @BindView(R.id.activity_lock_screen) View rootView;
  @Nullable @BindView(R.id.toolbar) Toolbar toolbar;
  @Nullable @BindView(R.id.appbar) AppBarLayout appBarLayout;

  @Nullable @Inject LockScreenPresenter presenter;
  @Nullable @Inject LockViewDelegate lockViewDelegate;

  @Nullable private DataHolderFragment<Long> ignoreDataHolder;
  @Nullable private DataHolderFragment<Boolean> excludeDataHolder;
  @Nullable private MenuItem menuIgnoreNone;
  @Nullable private MenuItem menuIgnoreFive;
  @Nullable private MenuItem menuIgnoreTen;
  @Nullable private MenuItem menuIgnoreThirty;
  @Nullable private MenuItem menuExclude;
  @Nullable private Unbinder unbinder;
  private int failCount;

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  @Override public void setDisplayName(@NonNull String name) {
    final ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setTitle(name);
    }
  }

  @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light_Lock);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lock);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    // Init holders here to avoid IllegalStateException
    ignoreDataHolder = DataHolderFragment.getInstance(this, "ignore_data");
    excludeDataHolder = DataHolderFragment.getInstance(this, "exclude_data");

    unbinder = ButterKnife.bind(this);

    // Inject Dagger graph
    DaggerLockScreenComponent.builder()
        .padLockComponent(PadLock.padLockComponent(this))
        .lockScreenModule(new LockScreenModule(this))
        .build()
        .inject(this);

    assert presenter != null;
    presenter.onCreateView(this);

    assert lockViewDelegate != null;
    lockViewDelegate.setTextColor(android.R.color.white);

    assert rootView != null;
    lockViewDelegate.onCreateView(presenter, this, rootView);

    ViewCompat.setElevation(appBarLayout, 0);
    setSupportActionBar(toolbar);
    failCount = 0;
  }

  @Override protected void onStart() {
    super.onStart();
    Timber.d("onStart");

    assert presenter != null;
    presenter.loadDisplayNameFromPackage();

    assert lockViewDelegate != null;
    lockViewDelegate.onStart(presenter);

    supportInvalidateOptionsMenu();
  }

  @Override public void onBackPressed() {
    Timber.d("onBackPressed");
    getApplicationContext().startActivity(home);
  }

  @Override protected boolean shouldConfirmBackPress() {
    return false;
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    assert presenter != null;
    presenter.onDestroyView();

    assert lockViewDelegate != null;
    lockViewDelegate.onDestroyView();

    assert unbinder != null;
    unbinder.unbind();

    failCount = 0;
  }

  @Override public void finish() {
    Timber.d("Finishing LockActivity");
    super.finish();
    overridePendingTransition(0, 0);
  }

  @NonNull @Override public String getPackageName() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getPackageName();
  }

  @NonNull @Override public String getActivityName() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getActivityName();
  }

  @NonNull @Override public String getCurrentAttempt() {
    assert lockViewDelegate != null;
    return lockViewDelegate.getCurrentAttempt();
  }

  private void showSnackbarWithText(String text) {
    assert rootView != null;
    final Snackbar snackbar = Snackbar.make(rootView, text, Snackbar.LENGTH_SHORT);
    final int defaultSnackColor = ContextCompat.getColor(this, R.color.snackbar);
    snackbar.getView().setBackgroundColor(defaultSnackColor);

    // KLUDGE Directly asks for the snackbar textview
    final TextView textView =
        (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
    if (textView != null) {
      textView.setBackgroundColor(defaultSnackColor);
      textView.setTextSize(14);
    }
    snackbar.show();
  }

  @Override public void onLocked() {
    showSnackbarWithText("This entry is temporarily locked");
  }

  @Override public void onLockedError() {
    Timber.e("LOCK ERROR");
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "lock_error");
  }

  @Override public void onSubmitSuccess() {
    Timber.d("Unlocked!");
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    PadLockService.passLockScreen();
    finish();
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    showSnackbarWithText("Error: Invalid PIN");

    ++failCount;

    // Once fail count is tripped once, continue to update it every time following until time elapses
    if (failCount > 2) {
      assert presenter != null;
      presenter.lockEntry();
    }
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    assert lockViewDelegate != null;
    lockViewDelegate.onRestoreInstanceState(savedInstanceState);
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    if (isChangingConfigurations()) {
      assert lockViewDelegate != null;
      lockViewDelegate.onSaveInstanceState(outState);

      assert ignoreDataHolder != null;
      ignoreDataHolder.put(0, getIgnorePeriodTime());

      assert excludeDataHolder != null;
      excludeDataHolder.put(0, shouldExcludeEntry());
    } else {
      DataHolderFragment.remove(this, Long.class);
      DataHolderFragment.remove(this, Boolean.class);
    }

    Timber.d("onSaveInstanceState");
    super.onSaveInstanceState(outState);
  }

  @Override public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    Timber.d("onCreateOptionsMenu");
    getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
    menuIgnoreNone = menu.findItem(R.id.menu_ignore_none);
    menuIgnoreFive = menu.findItem(R.id.menu_ignore_five);
    menuIgnoreTen = menu.findItem(R.id.menu_ignore_ten);
    menuIgnoreThirty = menu.findItem(R.id.menu_ignore_thirty);
    menuExclude = menu.findItem(R.id.menu_exclude);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
    // Set the default checked value
    assert ignoreDataHolder != null;
    final Long ignorePeriod = ignoreDataHolder.pop(0);

    assert presenter != null;
    presenter.setIgnorePeriodFromPreferences(ignorePeriod);

    assert excludeDataHolder != null;
    final Boolean exclude = excludeDataHolder.pop(0);
    if (exclude != null) {
      assert menuExclude != null;
      menuExclude.setChecked(exclude);
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override public void onSubmitError() {
    assert lockViewDelegate != null;
    lockViewDelegate.clearDisplay();
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "unlock_error");
  }

  @Override public void setIgnoreTimeNone() {
    if (menuIgnoreNone != null) {
      menuIgnoreNone.setChecked(true);
    }
  }

  @Override public void setIgnoreTimeFive() {
    if (menuIgnoreFive != null) {
      menuIgnoreFive.setChecked(true);
    }
  }

  @Override public void setIgnoreTimeTen() {
    if (menuIgnoreTen != null) {
      menuIgnoreTen.setChecked(true);
    }
  }

  @Override public void setIgnoreTimeThirty() {
    if (menuIgnoreThirty != null) {
      menuIgnoreThirty.setChecked(true);
    }
  }

  @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    Timber.d("onOptionsItemSelected");
    boolean handled;
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.menu_lockscreen_forgot:
        showForgotPasscodeDialog();
        handled = true;
        break;
      case R.id.menu_ignore_none:
        item.setChecked(true);
        handled = true;
        break;
      case R.id.menu_ignore_five:
        item.setChecked(true);
        handled = true;
        break;
      case R.id.menu_ignore_ten:
        item.setChecked(true);
        handled = true;
        break;
      case R.id.menu_ignore_thirty:
        item.setChecked(true);
        handled = true;
        break;
      case R.id.menu_exclude:
        item.setChecked(!item.isChecked());
        handled = true;
        break;
      case R.id.menu_lockscreen_info:
        showInfoDialog();
        handled = true;
        break;
      default:
        handled = false;
    }
    return handled || super.onOptionsItemSelected(item);
  }

  private void showInfoDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(),
        InfoDialog.newInstance(getPackageName(), getActivityName()), "info_dialog");
  }

  private void showForgotPasscodeDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new ForgotPasswordDialog(),
        FORGOT_PASSWORD_TAG);
  }

  @CheckResult @Override public long getIgnorePeriodTime() {
    assert presenter != null;
    if (menuIgnoreFive != null && menuIgnoreTen != null && menuIgnoreThirty != null) {
      if (menuIgnoreFive.isChecked()) {
        return presenter.getIgnoreTimeFive();
      } else if (menuIgnoreTen.isChecked()) {
        return presenter.getIgnoreTimeTen();
      } else if (menuIgnoreThirty.isChecked()) {
        return presenter.getIgnoreTimeThirty();
      }
    }
    return presenter.getIgnoreTimeNone();
  }

  @CheckResult @Override public boolean shouldExcludeEntry() {
    assert menuExclude != null;
    return menuExclude.isChecked();
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    assert lockViewDelegate != null;
    lockViewDelegate.onApplicationIconLoadedSuccess(icon);
  }

  @Override public void onApplicationIconLoadedError() {
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "error");
  }
}
