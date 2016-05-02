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
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.model.event.LockButtonClickEvent;
import com.pyamsoft.pydroid.base.PresenterImplBase;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public abstract class LockPresenterImpl<I extends LockView> extends PresenterImplBase<I>
    implements LockPresenter<I> {

  @NonNull private final LockInteractor lockInteractor;
  @NonNull private final Context appContext;

  protected LockPresenterImpl(final @NonNull Context context,
      @NonNull final LockInteractor lockInteractor) {
    this.appContext = context.getApplicationContext();
    this.lockInteractor = lockInteractor;
  }

  @Override public final Observable<Drawable> loadPackageIcon(final @NonNull String packageName) {
    return Observable.defer(
        () -> Observable.just(lockInteractor.loadPackageIcon(appContext, packageName)))
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread());
  }

  @Override public final void setDefaultDisplay() throws NullPointerException {
    get().setDefaultDisplay(LockInteractor.DEFAULT_STRING);
  }

  private void processClickEvent(@NonNull final LockButtonClickEvent event)
      throws NullPointerException {
    final int status = event.status();
    final String code = event.code();

    get().setDefaultDisplay(code);
    switch (status) {
      case LockButtonClickEvent.STATUS_SUBMITTABLE:
        Timber.d("STATUS_SUBMITTABLE, attempt submission");
        get().onSubmitButtonEvent();
        break;
      case LockButtonClickEvent.STATUS_COMMAND:
        Timber.d("STATUS_COMMAND, handle command in view");
        get().onCommandButtonEvent();
        break;
      case LockButtonClickEvent.STATUS_ERROR:
        Timber.d("STATUS_ERROR, handle error in view");
        get().onErrorButtonEvent();
        break;
      default:
        Timber.d("STATUS_NONE, do nothing");
        get().onNormalButtonEvent();
    }
  }

  @Override public final void clickButton1() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.ONE);
    processClickEvent(event);
  }

  @Override public final void clickButton2() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.TWO);
    processClickEvent(event);
  }

  @Override public final void clickButton3() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.THREE);
    processClickEvent(event);
  }

  @Override public final void clickButton4() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.FOUR);
    processClickEvent(event);
  }

  @Override public final void clickButton5() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.FIVE);
    processClickEvent(event);
  }

  @Override public final void clickButton6() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.SIX);
    processClickEvent(event);
  }

  @Override public final void clickButton7() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.SEVEN);
    processClickEvent(event);
  }

  @Override public final void clickButton8() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.EIGHT);
    processClickEvent(event);
  }

  @Override public final void clickButton9() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.NINE);
    processClickEvent(event);
  }

  @Override public final void clickButton0() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.ZERO);
    processClickEvent(event);
  }

  @Override public final void clickButtonBack() throws NullPointerException {
    final LockButtonClickEvent event = clickButton(LockInteractor.BACK);
    processClickEvent(event);
  }

  @Override public final void clickButtonCommand() throws NullPointerException {
    final LockButtonClickEvent event = takeCommand();
    processClickEvent(event);
  }

  protected final String buttonClicked(final char button, String attempt)
      throws NullPointerException {
    String code;
    switch (button) {
      // BACK
      case LockInteractor.DEFAULT_CHAR:
        code = lockInteractor.deleteFromAttempt(attempt);
        break;
      default:
        code = lockInteractor.appendToAttempt(attempt, button);
    }
    return code;
  }

  protected abstract LockButtonClickEvent takeCommand() throws NullPointerException;

  protected abstract LockButtonClickEvent clickButton(char button) throws NullPointerException;
}
