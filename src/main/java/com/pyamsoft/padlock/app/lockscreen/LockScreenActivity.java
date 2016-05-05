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
package com.pyamsoft.padlock.app.lockscreen;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.ErrorDialog;
import com.pyamsoft.padlock.app.lock.delegate.LockViewDelegate;
import com.pyamsoft.padlock.app.lock.delegate.LockViewDelegateImpl;
import com.pyamsoft.padlock.app.service.LockService;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.dagger.db.DBModule;
import com.pyamsoft.padlock.dagger.lockscreen.DaggerLockScreenComponent;
import com.pyamsoft.padlock.dagger.lockscreen.LockScreenModule;
import com.pyamsoft.pydroid.base.ActivityBase;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import timber.log.Timber;

public final class LockScreenActivity extends ActivityBase implements LockScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = LockViewDelegate.ENTRY_PACKAGE_NAME;
  @NonNull public static final String ENTRY_ACTIVITY_NAME = LockViewDelegate.ENTRY_ACTIVITY_NAME;
  @NonNull public static final String ENTRY_NAME = "entry_name";
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";

  @NonNull private final Intent home;
  @BindView(R.id.activity_lock_screen) View rootView;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbar) AppBarLayout appBarLayout;

  @Inject LockScreenPresenter presenter;
  private LockViewDelegate lockViewDelegate;

  private DataHolderFragment<Long> ignoreDataHolder;
  private DataHolderFragment<Boolean> excludeDataHolder;
  private String appName;
  private MenuItem menuIgnoreNone;
  private MenuItem menuIgnoreFive;
  private MenuItem menuIgnoreTen;
  private MenuItem menuIgnoreThirty;
  private MenuItem menuExclude;
  private int failCount;
  private Unbinder unbinder;

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  @Override public void onCreate(final Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light_Lock);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lock);

    // Init holders here to avoid IllegalStateException
    ignoreDataHolder = DataHolderFragment.getInstance(this, Long.class);
    excludeDataHolder = DataHolderFragment.getInstance(this, Boolean.class);

    unbinder = ButterKnife.bind(this);

    // Inject Dagger graph
    DaggerLockScreenComponent.builder()
        .lockScreenModule(new LockScreenModule())
        .dBModule(new DBModule())
        .padLockComponent(PadLock.padLockComponent(this))
        .build()
        .inject(this);

    lockViewDelegate = new LockViewDelegateImpl<>(presenter, new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (keyEvent == null) {
          Timber.e("KeyEvent was not caused by keypress");
          return false;
        }

        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
          Timber.d("KeyEvent is Enter pressed");
          presenter.unlockEntry();
          return true;
        }

        Timber.d("Do not handle key event");
        return false;
      }
    });
    presenter.create();
    presenter.bind(this);
    lockViewDelegate.onCreate(this, rootView);

    Timber.d("bind");
    getValuesFromIntent();

    ViewCompat.setElevation(appBarLayout, 0);
    setSupportActionBar(toolbar);
    failCount = 0;
  }

  private void getValuesFromIntent() {
    final Intent intent = getIntent();
    appName = intent.getStringExtra(ENTRY_NAME);
    Timber.d("Got value appName: %s", appName);
    Timber.d("reset fail count");
  }

  @Override protected void onStart() {
    super.onStart();
    Timber.d("onStart");
    final ActionBar bar = getSupportActionBar();
    if (bar != null) {
      bar.setTitle(appName);
    }
    lockViewDelegate.onStart();

    supportInvalidateOptionsMenu();
  }

  @Override public void onBackPressed() {
    Timber.d("onBackPressed");
    getApplicationContext().startActivity(home);
  }

  @Override protected boolean shouldConfirmBackPress() {
    return false;
  }

  @Override protected boolean isDonationSupported() {
    return false;
  }

  @Override public String getPlayStoreAppPackage() {
    return null;
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    presenter.unbind();
    presenter.destroy();
    lockViewDelegate.onDestroy();
    failCount = 0;
    if (unbinder != null) {
      unbinder.unbind();
    }
  }

  @Override public void finish() {
    Timber.d("Finishing LockActivity");
    super.finish();
    overridePendingTransition(0, 0);
  }

  @NonNull @Override public String getPackageName() {
    return lockViewDelegate.getPackageName();
  }

  @NonNull @Override public String getActivityName() {
    return lockViewDelegate.getActivityName();
  }

  @Override public void setImageSuccess(@NonNull Drawable drawable) {
    lockViewDelegate.setImageSuccess(drawable);
  }

  @Override public void setImageError() {
    lockViewDelegate.setImageError();
  }

  @NonNull @Override public String getCurrentAttempt() {
    return lockViewDelegate.getCurrentAttempt();
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
    final LockService service = PadLockService.getInstance();
    if (service != null) {
      service.passLockScreen();
    }
    finish();
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    showSnackbarWithText("Error: Invalid PIN");

    ++failCount;

    // Once fail count is tripped once, continue to update it every time following until time elapses
    if (failCount > 2) {
      presenter.lockEntry();
    }
  }

  @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    lockViewDelegate.onRestoreInstanceState(savedInstanceState);
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    if (isChangingConfigurations()) {
      lockViewDelegate.onSaveInstanceState(outState);
      ignoreDataHolder.put(0, getIgnorePeriodTime());
      excludeDataHolder.put(0, shouldExcludeEntry());
    } else {
      DataHolderFragment.remove(this, Long.class);
      DataHolderFragment.remove(this, Boolean.class);
    }

    Timber.d("onSaveInstanceState");
    super.onSaveInstanceState(outState);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    Timber.d("onCreateOptionsMenu");
    getMenuInflater().inflate(R.menu.lockscreen_menu, menu);
    menuIgnoreNone = menu.findItem(R.id.menu_ignore_none);
    menuIgnoreFive = menu.findItem(R.id.menu_ignore_five);
    menuIgnoreTen = menu.findItem(R.id.menu_ignore_ten);
    menuIgnoreThirty = menu.findItem(R.id.menu_ignore_thirty);
    menuExclude = menu.findItem(R.id.menu_exclude);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
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
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "unlock_error");
  }

  @Override public void setIgnoreTimeError() {
    Timber.e("Failed to set ignore time");
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new ErrorDialog(),
        "ignore_time");
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

  @Override public boolean onOptionsItemSelected(MenuItem item) {
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

  @Override public long getIgnorePeriodTime() {
    if (menuIgnoreFive != null && menuIgnoreTen != null && menuIgnoreThirty != null) {
      if (menuIgnoreFive.isChecked()) {
        return PadLockPreferences.PERIOD_FIVE;
      } else if (menuIgnoreTen.isChecked()) {
        return PadLockPreferences.PERIOD_TEN;
      } else if (menuIgnoreThirty.isChecked()) {
        return PadLockPreferences.PERIOD_THIRTY;
      }
    }
    return PadLockPreferences.PERIOD_NONE;
  }

  @Override public boolean shouldExcludeEntry() {
    return menuExclude.isChecked();
  }
}
