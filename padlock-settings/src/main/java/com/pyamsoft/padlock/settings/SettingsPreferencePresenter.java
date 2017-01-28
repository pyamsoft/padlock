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

package com.pyamsoft.padlock.settings;

import android.support.annotation.NonNull;
import com.pyamsoft.pydroid.presenter.Presenter;

interface SettingsPreferencePresenter extends Presenter<Presenter.Empty> {

  void requestClearAll(@NonNull RequestCallback callback);

  void requestClearDatabase(@NonNull RequestCallback callback);

  void processClearRequest(int type, @NonNull ClearCallback callback);

  void setApplicationInstallReceiverState();

  interface ClearCallback {
    void onClearAll();

    void onClearDatabase();
  }

  interface RequestCallback {

    void showConfirmDialog(int type);
  }
}
