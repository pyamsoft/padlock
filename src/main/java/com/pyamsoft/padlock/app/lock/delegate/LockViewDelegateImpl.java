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
package com.pyamsoft.padlock.app.lock.delegate;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.pyamsoft.padlock.R;
import com.pyamsoft.pydroid.tool.AsyncVectorDrawableTask;
import com.pyamsoft.padlock.app.lock.LockPresenter;
import com.pyamsoft.padlock.app.lock.LockView;
import com.pyamsoft.pydroid.model.AsyncDrawable;
import java.lang.ref.WeakReference;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public final class LockViewDelegateImpl<I extends LockView, P extends LockPresenter<I>>
    implements LockViewDelegate {

  @NonNull private static final String CODE_DISPLAY = "CODE_DISPLAY";

  @NonNull private final P presenter;
  @NonNull private final WeakReference<LockView> weakView;
  @BindView(R.id.lock_pin_field) TextView codeDisplay;
  @BindView(R.id.lock_1) Button button1;
  @BindView(R.id.lock_2) Button button2;
  @BindView(R.id.lock_3) Button button3;
  @BindView(R.id.lock_4) Button button4;
  @BindView(R.id.lock_5) Button button5;
  @BindView(R.id.lock_6) Button button6;
  @BindView(R.id.lock_7) Button button7;
  @BindView(R.id.lock_8) Button button8;
  @BindView(R.id.lock_9) Button button9;
  @BindView(R.id.lock_0) Button button0;
  @BindView(R.id.lock_back) ImageView buttonBack;
  @BindView(R.id.lock_command) ImageView buttonCommand;
  @NonNull private Subscription imageSubscription = Subscriptions.empty();
  private String activityName;
  private String packageName;
  private AsyncVectorDrawableTask backIconTask;
  private AsyncVectorDrawableTask commandIconTask;
  private Unbinder unbinder;

  public LockViewDelegateImpl(final I lockView, final @NonNull P presenter) {
    weakView = new WeakReference<>(lockView);
    this.presenter = presenter;
  }

  @Override public void onCreate(final Activity activity, final View rootView) {
    onCreate(rootView, activity.getIntent().getExtras());
  }

  @Override public void onCreate(final Fragment fragment, final View rootView) {
    onCreate(rootView, fragment.getArguments());
  }

  private void onCreate(final View rootView, final Bundle bundle) {
    unbinder = ButterKnife.bind(this, rootView);
    getValuesFromBundle(bundle);
    setupButtonPad(rootView);
  }

  @Override public void setDefaultDisplay(String defaultCode) {
    Timber.d("setDefaultDisplay");
    codeDisplay.setText(defaultCode);
  }

  private void setupButtonPad(View rootView) {
    button1.setOnClickListener(view -> presenter.clickButton1());
    button2.setOnClickListener(view -> presenter.clickButton2());
    button3.setOnClickListener(view -> presenter.clickButton3());
    button4.setOnClickListener(view -> presenter.clickButton4());
    button5.setOnClickListener(view -> presenter.clickButton5());
    button6.setOnClickListener(view -> presenter.clickButton6());
    button7.setOnClickListener(view -> presenter.clickButton7());
    button8.setOnClickListener(view -> presenter.clickButton8());
    button9.setOnClickListener(view -> presenter.clickButton9());
    button0.setOnClickListener(view -> presenter.clickButton0());
    buttonBack.setOnClickListener(view -> presenter.clickButtonBack());
    buttonCommand.setOnClickListener(view -> presenter.clickButtonCommand());

    final LockView lockView = weakView.get();
    if (lockView != null) {
      backIconTask = new AsyncVectorDrawableTask(buttonBack);
      commandIconTask = new AsyncVectorDrawableTask(buttonCommand);

      backIconTask.execute(
          new AsyncDrawable(rootView.getContext().getApplicationContext(), lockView.getBackIcon()));
      commandIconTask.execute(new AsyncDrawable(rootView.getContext().getApplicationContext(),
          lockView.getCommandIcon()));
    }

    rootView.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override public void onGlobalLayout() {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            final float buttonWidth = rootView.getWidth() * 0.09F;
            final float buttonHeight = rootView.getHeight() * 0.08F;

            final float buttonSize = Math.min(buttonWidth, buttonHeight);
            button1.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button2.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button3.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button4.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button5.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button6.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button7.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button8.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button9.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
            button0.setTextSize(TypedValue.COMPLEX_UNIT_PX, buttonSize);
          }
        });
  }

  private void getValuesFromBundle(Bundle bundle) {
    activityName = bundle.getString(ENTRY_ACTIVITY_NAME);
    packageName = bundle.getString(ENTRY_PACKAGE_NAME);
    Timber.d("Got value activityName: %s", activityName);
    Timber.d("Got value packageName: %s", packageName);
  }

  @Override public void onStart() {
    unsubImage();
    imageSubscription = presenter.loadPackageIcon(packageName).subscribe(drawable -> {
      codeDisplay.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
      presenter.setDefaultDisplay();
    }, throwable -> {
      Timber.e(throwable, "onError");
    });
  }

  private void unsubImage() {
    if (!imageSubscription.isUnsubscribed()) {
      imageSubscription.unsubscribe();
    }
  }

  @Override public void onDestroy() {
    unsubImage();
  }

  @Override public void onDestroyView() {
    if (unbinder != null) {
      unbinder.unbind();
    }

    if (backIconTask != null) {
      if (!backIconTask.isCancelled()) {
        backIconTask.cancel(true);
      }
      backIconTask = null;
    }

    if (commandIconTask != null) {
      if (!commandIconTask.isCancelled()) {
        commandIconTask.cancel(true);
      }
      commandIconTask = null;
    }
  }

  @NonNull @Override public String getCurrentAttempt() {
    return codeDisplay.getText().toString();
  }

  @NonNull @Override public String getPackageName() {
    return packageName;
  }

  @NonNull @Override public String getActivityName() {
    return activityName;
  }

  @Override public void onRestoreInstanceState(Bundle savedInstanceState) {
    Timber.d("onRestoreInstanceState");
    final String attempt = savedInstanceState.getString(CODE_DISPLAY, null);
    if (attempt == null) {
      presenter.setDefaultDisplay();
    } else {
      codeDisplay.setText(attempt);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    Timber.d("onSaveInstanceState");
    final String attempt = getCurrentAttempt();
    outState.putString(CODE_DISPLAY, attempt);
  }
}
