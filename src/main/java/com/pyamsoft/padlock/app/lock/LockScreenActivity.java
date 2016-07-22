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
import com.pyamsoft.padlock.app.base.AppIconLoaderPresenter;
import com.pyamsoft.padlock.app.base.ErrorDialog;
import com.pyamsoft.pydroid.base.activity.NoDonationActivityBase;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public abstract class LockScreenActivity extends NoDonationActivityBase
    implements LockScreen, LockViewDelegate.Callback {

  @NonNull public static final String ENTRY_PACKAGE_NAME = LockViewDelegate.ENTRY_PACKAGE_NAME;
  @NonNull public static final String ENTRY_ACTIVITY_NAME = LockViewDelegate.ENTRY_ACTIVITY_NAME;
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";

  @NonNull private final Intent home;
  @BindView(R.id.activity_lock_screen) View rootView;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbar) AppBarLayout appBarLayout;

  @Inject AppIconLoaderPresenter<LockScreen> appIconLoaderPresenter;
  @Inject LockScreenPresenter presenter;
  @Inject LockViewDelegate lockViewDelegate;

  private DataHolderFragment<Long> ignoreDataHolder;
  private DataHolderFragment<Boolean> excludeDataHolder;
  private MenuItem menuIgnoreNone;
  private MenuItem menuIgnoreOne;
  private MenuItem menuIgnoreFive;
  private MenuItem menuIgnoreTen;
  private MenuItem menuIgnoreFifteen;
  private MenuItem menuIgnoreTwenty;
  private MenuItem menuIgnoreThirty;
  private MenuItem menuIgnoreFourtyFive;
  private MenuItem menuIgnoreSixty;
  private MenuItem menuExclude;
  private Unbinder unbinder;

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  @Override public void setDisplayName(@NonNull String name) {
    Timber.d("Set toolbar name %s", name);
    toolbar.setTitle(name);
    final ActionBar bar = getSupportActionBar();
    if (bar != null) {
      Timber.d("Set actionbar name %s", name);
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
    PadLock.getInstance().getPadLockComponent().plusLockScreen().inject(this);

    presenter.bindView(this);
    appIconLoaderPresenter.bindView(this);
    lockViewDelegate.setTextColor(android.R.color.black);
    lockViewDelegate.onCreateView(this, this, rootView);

    setSupportActionBar(toolbar);
  }

  @Override protected void onStart() {
    super.onStart();
    Timber.d("onStart");

    presenter.loadDisplayNameFromPackage(lockViewDelegate.getAppPackageName());
    lockViewDelegate.onStart(appIconLoaderPresenter);
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
    Timber.d("onDestroy");

    presenter.unbindView();
    appIconLoaderPresenter.unbindView();
    lockViewDelegate.onDestroyView();
    unbinder.unbind();

    Timber.d("Clear currently locked");
  }

  @Override public void finish() {
    super.finish();
    Timber.d("Finish");
    overridePendingTransition(0, 0);
  }

  private void showSnackbarWithText(String text) {
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
    lockViewDelegate.clearDisplay();
    presenter.postUnlock(lockViewDelegate.getAppPackageName(),
        lockViewDelegate.getAppActivityName(), menuExclude.isChecked(),
        getSelectedIgnoreTimeIndex());
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    lockViewDelegate.clearDisplay();
    showSnackbarWithText("Error: Invalid PIN");

    // Once fail count is tripped once, continue to update it every time following until time elapses
    presenter.lockEntry(lockViewDelegate.getAppPackageName(),
        lockViewDelegate.getAppActivityName());
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    lockViewDelegate.onRestoreInstanceState(savedInstanceState);
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    if (isChangingConfigurations()) {
      lockViewDelegate.onSaveInstanceState(outState);
      presenter.saveSelectedOptions(getSelectedIgnoreTimeIndex());
    }

    Timber.d("onSaveInstanceState");
    super.onSaveInstanceState(outState);
  }

  @Override public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    Timber.d("onCreateOptionsMenu");
    getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
    menuIgnoreNone = menu.findItem(R.id.menu_ignore_none);
    menuIgnoreOne = menu.findItem(R.id.menu_ignore_one);
    menuIgnoreFive = menu.findItem(R.id.menu_ignore_five);
    menuIgnoreTen = menu.findItem(R.id.menu_ignore_ten);
    menuIgnoreFifteen = menu.findItem(R.id.menu_ignore_fifteen);
    menuIgnoreTwenty = menu.findItem(R.id.menu_ignore_twenty);
    menuIgnoreThirty = menu.findItem(R.id.menu_ignore_thirty);
    menuIgnoreFourtyFive = menu.findItem(R.id.menu_ignore_fourtyfive);
    menuIgnoreSixty = menu.findItem(R.id.menu_ignore_sixty);
    menuExclude = menu.findItem(R.id.menu_exclude);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
    // Set the default checked value
    final Long ignorePeriod = ignoreDataHolder.pop(0);
    presenter.setIgnorePeriodFromPreferences(ignorePeriod);

    final Boolean exclude = excludeDataHolder.pop(0);
    if (exclude != null) {
      menuExclude.setChecked(exclude);
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override public void onSubmitError() {
    lockViewDelegate.clearDisplay();
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "unlock_error");
  }

  @Override public void setIgnoreTimeNone() {
    if (menuIgnoreNone != null) {
      menuIgnoreNone.setChecked(true);
    }
  }

  @Override public void setIgnoreTimeOne() {
    menuIgnoreOne.setChecked(true);
  }

  @Override public void setIgnoreTimeFive() {
    menuIgnoreFive.setChecked(true);
  }

  @Override public void setIgnoreTimeTen() {
    menuIgnoreTen.setChecked(true);
  }

  @Override public void setIgnoreTimeFifteen() {
    menuIgnoreFifteen.setChecked(true);
  }

  @Override public void setIgnoreTimeTwenty() {
    menuIgnoreTwenty.setChecked(true);
  }

  @Override public void setIgnoreTimeThirty() {
    menuIgnoreThirty.setChecked(true);
  }

  @Override public void setIgnoreTimeFourtyFive() {
    menuIgnoreFourtyFive.setChecked(true);
  }

  @Override public void setIgnoreTimeSixty() {
    menuIgnoreSixty.setChecked(true);
  }

  @Override public void onSaveMenuSelections(long ignoreTime) {
    ignoreDataHolder.put(0, ignoreTime);
    excludeDataHolder.put(0, menuExclude.isChecked());
  }

  @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    Timber.d("onOptionsItemSelected");
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.menu_lockscreen_forgot:
        showForgotPasscodeDialog();
        break;
      case R.id.menu_exclude:
        item.setChecked(!item.isChecked());
        break;
      case R.id.menu_lockscreen_info:
        showInfoDialog();
        break;
      default:
        item.setChecked(true);
    }
    return true;
  }

  @CheckResult private int getSelectedIgnoreTimeIndex() {
    int index;
    if (menuIgnoreOne.isChecked()) {
      index = 1;
    } else if (menuIgnoreFive.isChecked()) {
      index = 2;
    } else if (menuIgnoreTen.isChecked()) {
      index = 3;
    } else if (menuIgnoreFifteen.isChecked()) {
      index = 4;
    } else if (menuIgnoreTwenty.isChecked()) {
      index = 5;
    } else if (menuIgnoreThirty.isChecked()) {
      index = 6;
    } else if (menuIgnoreFourtyFive.isChecked()) {
      index = 7;
    } else if (menuIgnoreSixty.isChecked()) {
      index = 8;
    } else {
      index = 0;
    }
    return index;
  }

  private void showInfoDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(),
        InfoDialog.newInstance(lockViewDelegate.getAppPackageName(),
            lockViewDelegate.getAppActivityName()), "info_dialog");
  }

  private void showForgotPasscodeDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new ForgotPasswordDialog(),
        FORGOT_PASSWORD_TAG);
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    lockViewDelegate.onApplicationIconLoadedSuccess(icon);
  }

  @Override public void onApplicationIconLoadedError() {
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "error");
  }

  @Override public void onSubmitPressed() {
    presenter.submit(lockViewDelegate.getAppPackageName(), lockViewDelegate.getAppActivityName(),
        lockViewDelegate.getCurrentAttempt());
  }
}
