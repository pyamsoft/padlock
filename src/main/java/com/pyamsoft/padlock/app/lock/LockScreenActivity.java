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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.ErrorDialog;
import com.pyamsoft.pydroid.base.ActivityBase;
import com.pyamsoft.pydroid.base.PersistLoader;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import java.util.Locale;
import rx.Subscription;
import timber.log.Timber;

public abstract class LockScreenActivity extends ActivityBase implements LockScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull public static final String ENTRY_REAL_NAME = "real_name";
  @NonNull public static final String ENTRY_LOCK_CODE = "lock_code";
  @NonNull public static final String ENTRY_IS_SYSTEM = "is_system";
  @NonNull public static final String ENTRY_LOCK_UNTIL_TIME = "lock_until_time";
  @NonNull public static final String ENTRY_IGNORE_UNTIL_TIME = "ignore_until_time";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";
  @NonNull private static final String KEY_LOCK_PRESENTER = "key_lock_presenter";

  @NonNull private final Intent home;
  @NonNull private final AsyncDrawableMap taskMap;
  @BindView(R.id.activity_lock_screen) CoordinatorLayout rootView;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.lock_image) ImageView image;
  @BindView(R.id.lock_text) TextInputLayout textLayout;
  @BindView(R.id.lock_image_go) ImageView imageGo;
  @BindView(R.id.lock_display_hint) TextView hintDisplay;

  @SuppressWarnings("WeakerAccess") LockScreenPresenter presenter;
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  @SuppressWarnings("WeakerAccess") String lockedActivityName;
  private long ignorePeriod = -1;
  private boolean exclude;
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
  private EditText editText;
  private Unbinder unbinder;
  private String lockedPackageName;
  private String lockedRealName;
  private String lockedCode;
  private boolean lockedSystem;
  private long[] ignoreTimes;
  private long loadedKey;
  private long lockUntilTime;
  private long ignoreUntilTime;

  LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    taskMap = new AsyncDrawableMap();
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

    loadedKey = PersistentCache.get()
        .load(KEY_LOCK_PRESENTER, savedInstanceState,
            new PersistLoader.Callback<LockScreenPresenter>() {
              @NonNull @Override public PersistLoader<LockScreenPresenter> createLoader() {
                return new LockScreenPresenterLoader(getApplicationContext());
              }

              @Override public void onPersistentLoaded(@NonNull LockScreenPresenter persist) {
                presenter = persist;
              }
            });

    unbinder = ButterKnife.bind(this);

    final String[] stringIgnoreTimes =
        getApplicationContext().getResources().getStringArray(R.array.ignore_time_entries);
    ignoreTimes = new long[stringIgnoreTimes.length];
    for (int i = 0; i < stringIgnoreTimes.length; ++i) {
      ignoreTimes[i] = Long.parseLong(stringIgnoreTimes[i]);
    }

    getValuesFromBundle();

    editText = textLayout.getEditText();
    if (editText == null) {
      throw new NullPointerException("Edit text is NULL");
    }

    editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
      if (keyEvent == null) {
        Timber.e("KeyEvent was not caused by keypress");
        return false;
      }

      if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && actionId == EditorInfo.IME_NULL) {
        Timber.d("KeyEvent is Enter pressed");
        presenter.submit(lockedPackageName, lockedActivityName, lockedCode, lockUntilTime,
            getCurrentAttempt());
        return true;
      }

      Timber.d("Do not handle key event");
      return false;
    });

    // Force the keyboard
    imm =
        (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

    imageGo.setOnClickListener(view -> {
      presenter.submit(lockedPackageName, lockedActivityName, lockedCode, lockUntilTime,
          getCurrentAttempt());
      imm.toggleSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0, 0);
    });

    // Force keyboard focus
    editText.requestFocus();

    editText.setOnFocusChangeListener((view, hasFocus) -> {
      if (hasFocus) {
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
      }
    });

    final Subscription arrowGoTask =
        AsyncDrawable.with(this).load(R.drawable.ic_arrow_forward_24dp).into(imageGo);
    taskMap.put("arrow", arrowGoTask);

    clearDisplay();

    setSupportActionBar(toolbar);

    // Hide hint to begin with
    hintDisplay.setVisibility(View.GONE);
  }

  @Override public void setDisplayHint(@NonNull String hint) {
    Timber.d("Settings hint");
    hintDisplay.setText(
        String.format(Locale.getDefault(), "Hint: %s", hint.isEmpty() ? "NO HINT" : hint));
  }

  private void clearDisplay() {
    editText.setText("");
  }

  private void getValuesFromBundle() {
    final Bundle bundle = getIntent().getExtras();
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME);
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    lockedRealName = bundle.getString(ENTRY_REAL_NAME);
    lockedCode = bundle.getString(ENTRY_LOCK_CODE);
    lockUntilTime = bundle.getLong(ENTRY_LOCK_UNTIL_TIME, 0);
    ignoreUntilTime = bundle.getLong(ENTRY_IGNORE_UNTIL_TIME, 0);
    lockedSystem = bundle.getBoolean(ENTRY_IS_SYSTEM, false);

    if (lockedPackageName == null || lockedActivityName == null || lockedRealName == null) {
      throw new NullPointerException("Missing required lock attributes");
    }

    // Reload options
    supportInvalidateOptionsMenu();
  }

  @Override protected void onStart() {
    super.onStart();
    Timber.d("onStart");
    presenter.bindView(this);
    presenter.displayLockedHint();

    presenter.loadDisplayNameFromPackage(lockedPackageName);
    presenter.loadApplicationIcon(lockedPackageName);
    supportInvalidateOptionsMenu();
  }

  @Override protected void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onBackPressed() {
    Timber.d("onBackPressed");
    getApplicationContext().startActivity(home);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Timber.d("onDestroy");
    if (!isChangingConfigurations()) {
      PersistentCache.get().unload(loadedKey);
    }

    Timber.d("Clear currently locked");
    taskMap.clear();
    imm.toggleSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0, 0);
    unbinder.unbind();
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

  @Override public void onLocked(long lockUntilTime) {
    this.lockUntilTime = lockUntilTime;
    getIntent().removeExtra(ENTRY_LOCK_UNTIL_TIME);
    getIntent().putExtra(ENTRY_LOCK_UNTIL_TIME, lockUntilTime);
    showSnackbarWithText("This entry is temporarily locked");
  }

  @Override public void onLockedError() {
    Timber.e("LOCK ERROR");
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "lock_error");
  }

  @Override public void onSubmitSuccess() {
    Timber.d("Unlocked!");
    clearDisplay();

    presenter.postUnlock(lockedPackageName, lockedActivityName, lockedRealName, lockedCode,
        lockedSystem, menuExclude.isChecked(), getIgnoreTimeFromSelectedIndex());
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    clearDisplay();
    showSnackbarWithText("Error: Invalid PIN");
    hintDisplay.setVisibility(View.VISIBLE);

    // Once fail count is tripped once, continue to update it every time following until time elapses
    presenter.lockEntry(lockedPackageName, lockedActivityName, lockedCode, lockUntilTime,
        ignoreUntilTime, lockedSystem);
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    ignorePeriod = savedInstanceState.getLong("IGNORE", -1);
    exclude = savedInstanceState.getBoolean("EXCLUDE", false);
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    if (attempt == null) {
      Timber.d("Empty attempt");
      clearDisplay();
    } else {
      Timber.d("Set attempt %s", attempt);
      editText.setText(attempt);
    }
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    final String attempt = getCurrentAttempt();
    final long ignoreTime = getIgnoreTimeFromSelectedIndex();
    outState.putString(CODE_DISPLAY, attempt);
    outState.putLong("IGNORE", ignoreTime);
    outState.putBoolean("EXCLUDE", menuExclude.isChecked());
    PersistentCache.get().saveKey(outState, KEY_LOCK_PRESENTER, loadedKey);
    super.onSaveInstanceState(outState);
  }

  @SuppressWarnings("WeakerAccess") @CheckResult @NonNull String getCurrentAttempt() {
    return editText.getText().toString();
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
    if (presenter != null) {
      if (ignorePeriod == -1) {
        Timber.d("No previous selection, load ignore time from preference");
        presenter.createWithDefaultIgnoreTime();
      } else {
        initializeWithIgnoreTime(ignorePeriod);
      }
    }

    menuExclude.setChecked(exclude);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override public void onSubmitError() {
    clearDisplay();
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "unlock_error");
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

  @CheckResult private long getIgnoreTimeFromSelectedIndex() {
    int index;
    try {
      if (menuIgnoreNone.isChecked()) {
        index = 0;
      } else if (menuIgnoreOne.isChecked()) {
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
        throw new RuntimeException("Invalid index for option selection");
      }
    } catch (NullPointerException e) {
      Timber.e(e, "NULL menu item");
      index = 0;
    }

    return ignoreTimes[index];
  }

  @Override public void initializeWithIgnoreTime(long time) {
    if (time == ignoreTimes[0]) {
      menuIgnoreNone.setChecked(true);
    } else if (time == ignoreTimes[1]) {
      menuIgnoreOne.setChecked(true);
    } else if (time == ignoreTimes[2]) {
      menuIgnoreFive.setChecked(true);
    } else if (time == ignoreTimes[3]) {
      menuIgnoreTen.setChecked(true);
    } else if (time == ignoreTimes[4]) {
      menuIgnoreFifteen.setChecked(true);
    } else if (time == ignoreTimes[5]) {
      menuIgnoreTwenty.setChecked(true);
    } else if (time == ignoreTimes[6]) {
      menuIgnoreThirty.setChecked(true);
    } else if (time == ignoreTimes[7]) {
      menuIgnoreFourtyFive.setChecked(true);
    } else if (time == ignoreTimes[8]) {
      menuIgnoreSixty.setChecked(true);
    } else {
      throw new RuntimeException("Invalid index for ignore time: " + time);
    }
  }

  private void showInfoDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(),
        InfoDialog.newInstance(lockedPackageName, lockedActivityName), "info_dialog");
  }

  private void showForgotPasscodeDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new ForgotPasswordDialog(),
        FORGOT_PASSWORD_TAG);
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    image.setImageDrawable(icon);
  }

  @Override public void onApplicationIconLoadedError() {
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "error");
  }
}
