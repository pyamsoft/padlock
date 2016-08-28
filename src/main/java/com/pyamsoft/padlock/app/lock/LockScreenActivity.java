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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.util.AppUtil;
import java.util.Locale;
import rx.Subscription;
import timber.log.Timber;

public abstract class LockScreenActivity extends AppCompatActivity implements LockScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull public static final String ENTRY_REAL_NAME = "real_name";
  @NonNull public static final String ENTRY_LOCK_CODE = "lock_code";
  @NonNull public static final String ENTRY_IS_SYSTEM = "is_system";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";

  @NonNull final Intent home;
  @NonNull private final AsyncDrawableMap taskMap;
  @BindView(R.id.activity_lock_screen) CoordinatorLayout rootView;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appbar) AppBarLayout appBarLayout;
  @BindView(R.id.lock_image) ImageView image;
  @BindView(R.id.lock_text) TextInputLayout textLayout;
  @BindView(R.id.lock_image_go) ImageView imageGo;
  @BindView(R.id.lock_display_hint) TextView hintDisplay;

  LockScreenPresenter presenter;
  InputMethodManager imm;

  long ignorePeriod = -1;
  boolean exclude;

  MenuItem menuIgnoreNone;
  MenuItem menuIgnoreOne;
  MenuItem menuIgnoreFive;
  MenuItem menuIgnoreTen;
  MenuItem menuIgnoreFifteen;
  MenuItem menuIgnoreTwenty;
  MenuItem menuIgnoreThirty;
  MenuItem menuIgnoreFourtyFive;
  MenuItem menuIgnoreSixty;
  MenuItem menuExclude;
  EditText editText;
  Unbinder unbinder;
  String lockedPackageName;
  String lockedActivityName;
  String lockedRealName;
  String lockedCode;
  boolean lockedSystem;

  public LockScreenActivity() {
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

    unbinder = ButterKnife.bind(this);
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
        presenter.submit(lockedPackageName, lockedActivityName, getCurrentAttempt());
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
      presenter.submit(lockedPackageName, lockedActivityName, getCurrentAttempt());
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

    getSupportLoaderManager().initLoader(0, null,
        new LoaderManager.LoaderCallbacks<LockScreenPresenter>() {
          @Override public Loader<LockScreenPresenter> onCreateLoader(int id, Bundle args) {
            return new LockScreenPresenterLoader(getApplicationContext());
          }

          @Override
          public void onLoadFinished(Loader<LockScreenPresenter> loader, LockScreenPresenter data) {
            presenter = data;
          }

          @Override public void onLoaderReset(Loader<LockScreenPresenter> loader) {
            presenter = null;
          }
        });
  }

  @Override public void setDisplayHint(@NonNull String hint) {
    Timber.d("Settings hint");
    hintDisplay.setText(
        String.format(Locale.getDefault(), "Hint: %s", hint.isEmpty() ? "NO HINT" : hint));
  }

  public final void clearDisplay() {
    editText.setText("");
  }

  private void getValuesFromBundle() {
    final Bundle bundle = getIntent().getExtras();
    lockedPackageName = bundle.getString(ENTRY_PACKAGE_NAME);
    lockedActivityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    lockedRealName = bundle.getString(ENTRY_REAL_NAME);
    lockedCode = bundle.getString(ENTRY_LOCK_CODE);
    ignorePeriod = bundle.getLong("IGNORE", -1);
    exclude = bundle.getBoolean("EXCLUDE", false);

    final String lockedStringSystem = bundle.getString(ENTRY_IS_SYSTEM);

    if (lockedPackageName == null
        || lockedActivityName == null
        || lockedRealName == null
        || lockedStringSystem == null) {
      throw new NullPointerException("Missing required lock attributes");
    }

    // So that we can make sure a boolean is passed
    lockedSystem = Boolean.valueOf(lockedStringSystem);
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

  @Override public void onLocked() {
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
        lockedSystem, menuExclude.isChecked(), getSelectedIgnoreTimeIndex());
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    clearDisplay();
    showSnackbarWithText("Error: Invalid PIN");
    hintDisplay.setVisibility(View.VISIBLE);

    // Once fail count is tripped once, continue to update it every time following until time elapses
    presenter.lockEntry(lockedPackageName, lockedActivityName);
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
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
    if (isChangingConfigurations()) {
      final String attempt = getCurrentAttempt();
      outState.putString(CODE_DISPLAY, attempt);
      presenter.saveSelectedOptions(outState, getSelectedIgnoreTimeIndex());
    }

    Timber.d("onSaveInstanceState");
    super.onSaveInstanceState(outState);
  }

  @CheckResult @NonNull private String getCurrentAttempt() {
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
    // Set the default checked value
    presenter.setIgnorePeriodFromPreferences(ignorePeriod);
    menuExclude.setChecked(exclude);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override public void onSubmitError() {
    clearDisplay();
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

  @Override public void onSaveMenuSelections(@NonNull Bundle outState, long ignoreTime) {
    outState.putLong("IGNORE", ignoreTime);
    outState.putBoolean("EXCLUDE", menuExclude.isChecked());
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
