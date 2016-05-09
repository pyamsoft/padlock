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

package com.pyamsoft.padlock.app.settings;

import com.pyamsoft.pydroid.base.Presenter;

public interface SettingsPresenter extends Presenter<SettingsPresenter.SettingsView> {

  void setIgnorePeriodFromPreference();

  void setIgnorePeriodNone();

  void setIgnorePeriodFive();

  void setIgnorePeriodTen();

  void setIgnorePeriodThirty();

  void setTimeoutPeriodFromPreference();

  void setTimeoutPeriodNone();

  void setTimeoutPeriodOne();

  void setTimeoutPeriodFive();

  void setTimeoutPeriodTen();

  interface SettingsView {

    void setIgnorePeriodNone();

    void setIgnorePeriodFive();

    void setIgnorePeriodTen();

    void setIgnorePeriodThirty();

    void setTimeoutPeriodNone();

    void setTimeoutPeriodOne();

    void setTimeoutPeriodFive();

    void setTimeoutPeriodTen();
  }
}
