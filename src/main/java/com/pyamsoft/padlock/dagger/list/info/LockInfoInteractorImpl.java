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

package com.pyamsoft.padlock.dagger.list.info;

import android.content.Context;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.app.sql.PadLockOpenHelper;
import com.pyamsoft.padlock.dagger.lock.IconLoadInteractorImpl;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

final class LockInfoInteractorImpl extends IconLoadInteractorImpl implements LockInfoInteractor {

  @NonNull private final Context appContext;

  @Inject public LockInfoInteractorImpl(final @NonNull Context context) {
    super(context);
    appContext = context.getApplicationContext();
  }

  @NonNull @Override
  public Observable<List<PadLockEntry>> getActivityEntries(@NonNull String packageName) {
    return PadLockOpenHelper.queryWithPackageName(appContext, packageName).first();
  }
}
