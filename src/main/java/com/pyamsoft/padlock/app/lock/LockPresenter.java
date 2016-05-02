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

import android.graphics.drawable.Drawable;
import com.pyamsoft.pydroid.base.PresenterBase;
import rx.Observable;

public interface LockPresenter<I extends LockView> extends PresenterBase<I> {

  void clickButton1() throws NullPointerException;

  void clickButton2() throws NullPointerException;

  void clickButton3() throws NullPointerException;

  void clickButton4() throws NullPointerException;

  void clickButton5() throws NullPointerException;

  void clickButton6() throws NullPointerException;

  void clickButton7() throws NullPointerException;

  void clickButton8() throws NullPointerException;

  void clickButton9() throws NullPointerException;

  void clickButton0() throws NullPointerException;

  void clickButtonBack() throws NullPointerException;

  void clickButtonCommand() throws NullPointerException;

  void setDefaultDisplay() throws NullPointerException;

  Observable<Drawable> loadPackageIcon(String packageName);
}
