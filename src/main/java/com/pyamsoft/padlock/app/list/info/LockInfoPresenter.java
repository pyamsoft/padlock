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

package com.pyamsoft.padlock.app.list.info;

import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.list.ImageLoadPresenter;
import com.pyamsoft.padlock.app.list.ImageLoadView;
import com.pyamsoft.padlock.app.list.LockListCommon;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.pydroid.base.Presenter;
import java.util.List;

public interface LockInfoPresenter
    extends Presenter<LockInfoPresenter.LockInfoView>, ImageLoadPresenter {

  void populateList(@NonNull String packageName, @NonNull List<ActivityInfo> activities);

  interface LockInfoView extends LockListCommon, ImageLoadView {

    void onEntryAddedToList(@NonNull ActivityEntry entry);

    void onListPopulated();

    void onListPopulateError();
  }
}
