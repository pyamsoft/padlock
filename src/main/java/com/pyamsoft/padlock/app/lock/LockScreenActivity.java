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
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.list.ErrorDialog;
import com.pyamsoft.padlock.app.service.PadLockService;
import com.pyamsoft.padlock.databinding.ActivityLockBinding;
import com.pyamsoft.padlock.model.LockScreenEntry;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.activity.ActivityBase;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import com.pyamsoft.pydroidrx.RXLoader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import timber.log.Timber;

public class LockScreenActivity extends ActivityBase implements LockScreen {

  @NonNull public static final String ENTRY_PACKAGE_NAME = "entry_packagename";
  @NonNull public static final String ENTRY_ACTIVITY_NAME = "entry_activityname";
  @NonNull public static final String ENTRY_REAL_NAME = "real_name";
  @NonNull public static final String ENTRY_LOCK_CODE = "lock_code";
  @NonNull public static final String ENTRY_IS_SYSTEM = "is_system";
  @NonNull public static final String ENTRY_LOCK_UNTIL_TIME = "lock_until_time";
  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";
  @NonNull private static final String FORGOT_PASSWORD_TAG = "forgot_password";
  @NonNull private static final String KEY_LOCK_PRESENTER = "key_lock_presenter";

  /**
   * KLUDGE This is a map that holds references to Activities
   *
   * That's bad mk.
   *
   * More of a proof of concept, only publishable if it works just, really really well and
   * is LOCKED DOWN TO NOT LEAK
   */
  @NonNull private static final Map<LockScreenEntry, WeakReference<LockScreenActivity>>
      LOCK_SCREEN_MAP;

  static {
    LOCK_SCREEN_MAP = new HashMap<>();
  }

  @NonNull private final Intent home;
  @NonNull private final AsyncDrawable.Mapper taskMap;
  @SuppressWarnings("WeakerAccess") LockScreenPresenter presenter;
  @SuppressWarnings("WeakerAccess") InputMethodManager imm;
  @SuppressWarnings("WeakerAccess") String lockedActivityName;
  @SuppressWarnings("WeakerAccess") String lockedPackageName;
  @SuppressWarnings("WeakerAccess") String lockedCode;
  @SuppressWarnings("WeakerAccess") long lockUntilTime;
  private ActivityLockBinding binding;
  private long ignorePeriod = -1;
  private boolean excludeEntry;
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
  private long[] ignoreTimes;
  private long loadedKey;
  private String lockedRealName;
  private boolean lockedSystem;

  public LockScreenActivity() {
    home = new Intent(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    taskMap = new AsyncDrawable.Mapper();
  }

  static void addToLockedMap(@NonNull String packageName, @NonNull String className,
      @NonNull LockScreenActivity instance) {
    final LockScreenEntry entry = LockScreenEntry.create(packageName, className);
    if (LOCK_SCREEN_MAP.containsKey(entry)) {
      Timber.e("Instance for %s %s already exists. I hope you called finish before this",
          packageName, className);
    }

    Timber.i("Add instance to map for activity: %s %s", packageName, className);
    LOCK_SCREEN_MAP.put(entry, new WeakReference<>(instance));
  }

  @CheckResult @Nullable
  public static WeakReference<LockScreenActivity> hasLockedMapEntry(@NonNull String packageName,
      @NonNull String className) {
    final LockScreenEntry entry = LockScreenEntry.create(packageName, className);
    return LOCK_SCREEN_MAP.get(entry);
  }

  @Override public void setDisplayName(@NonNull String name) {
    Timber.d("Set toolbar name %s", name);
    binding.toolbar.setTitle(name);
    final ActionBar bar = getSupportActionBar();
    if (bar != null) {
      Timber.d("Set actionbar name %s", name);
      bar.setTitle(name);
    }
  }

  @CallSuper @Override public void onCreate(final @Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_PadLock_Light_Lock);
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_lock);
    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

    loadedKey = PersistentCache.get()
        .load(KEY_LOCK_PRESENTER, savedInstanceState,
            new PersistLoader.Callback<LockScreenPresenter>() {
              @NonNull @Override public PersistLoader<LockScreenPresenter> createLoader() {
                return new LockScreenPresenterLoader();
              }

              @Override public void onPersistentLoaded(@NonNull LockScreenPresenter persist) {
                presenter = persist;
              }
            });

    final String[] stringIgnoreTimes =
        getApplicationContext().getResources().getStringArray(R.array.ignore_time_entries);
    ignoreTimes = new long[stringIgnoreTimes.length];
    for (int i = 0; i < stringIgnoreTimes.length; ++i) {
      ignoreTimes[i] = Long.parseLong(stringIgnoreTimes[i]);
    }

    getValuesFromBundle();

    editText = binding.lockText.getEditText();
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

    binding.lockImageGo.setOnClickListener(view -> {
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

    final AsyncMap.Entry arrowGoTask = AsyncDrawable.with(this)
        .load(R.drawable.ic_arrow_forward_24dp, new RXLoader())
        .into(binding.lockImageGo);
    taskMap.put("arrow", arrowGoTask);

    clearDisplay();

    setSupportActionBar(binding.toolbar);

    // Hide hint to begin with
    binding.lockDisplayHint.setVisibility(View.GONE);
  }

  @Override public void setDisplayHint(@NonNull String hint) {
    Timber.d("Settings hint");
    binding.lockDisplayHint.setText(
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

    // Add the lock map
    addToLockedMap(lockedPackageName, lockedActivityName, this);
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

  @Override public void onBackPressed() {
    Timber.d("onBackPressed");
    getApplicationContext().startActivity(home);
  }

  @CallSuper @Override protected void onDestroy() {
    super.onDestroy();

    Timber.d("onDestroy");
    if (!isChangingConfigurations()) {
      PersistentCache.get().unload(loadedKey);
    }

    Timber.d("Clear currently locked");
    taskMap.clear();
    imm.toggleSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0, 0);
    binding.unbind();
  }

  @Override public void finish() {
    super.finish();
    Timber.d("Finish called, either from Us or from Outside");
    overridePendingTransition(0, 0);
  }

  private void showSnackbarWithText(@NonNull String text) {
    Snackbar.make(binding.activityLockScreen, text, Snackbar.LENGTH_SHORT).show();
  }

  @Override public void onPostUnlock() {
    Timber.d("POST Unlock Finished! 1");
    PadLockService.passLockScreen();
    finish();
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
        lockUntilTime, lockedSystem, menuExclude.isChecked(), getIgnoreTimeFromSelectedIndex());
  }

  @Override public void onSubmitFailure() {
    Timber.e("Failed to unlock");
    clearDisplay();
    showSnackbarWithText("Error: Invalid PIN");
    binding.lockDisplayHint.setVisibility(View.VISIBLE);

    // Once fail count is tripped once, continue to update it every time following until time elapses
    presenter.lockEntry(lockedPackageName, lockedActivityName, lockUntilTime);
  }

  @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    ignorePeriod = savedInstanceState.getLong("IGNORE", -1);
    excludeEntry = savedInstanceState.getBoolean("EXCLUDE", false);
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

    boolean exclude;
    try {
      exclude = menuExclude.isChecked();
    } catch (NullPointerException e) {
      exclude = false;
    }
    outState.putBoolean("EXCLUDE", exclude);

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
      if (ignorePeriod == -1 && presenter.isBound()) {
        Timber.d("No previous selection, load ignore time from preference");
        presenter.createWithDefaultIgnoreTime();
      } else {
        initializeWithIgnoreTime(ignorePeriod);
      }
    }

    menuExclude.setChecked(excludeEntry);
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
        index = 0;
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
      Timber.e("No valid ignore time, initialize to None");
      menuIgnoreNone.setChecked(true);
    }
  }

  private void showInfoDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(),
        LockedStatDialog.newInstance(binding.toolbar.getTitle().toString(), lockedPackageName,
            lockedActivityName, lockedRealName, lockedSystem, binding.lockImage.getDrawable()),
        "info_dialog");
  }

  private void showForgotPasscodeDialog() {
    AppUtil.guaranteeSingleDialogFragment(getSupportFragmentManager(), new ForgotPasswordDialog(),
        FORGOT_PASSWORD_TAG);
  }

  @Override public void onApplicationIconLoadedSuccess(@NonNull Drawable icon) {
    binding.lockImage.setImageDrawable(icon);
  }

  @Override public void onApplicationIconLoadedError() {
    AppUtil.guaranteeSingleDialogFragment(this, new ErrorDialog(), "error");
  }
}
