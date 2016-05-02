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
import com.pyamsoft.padlock.app.list.info.LockInfoInteractor;
import com.pyamsoft.padlock.app.lock.LockCommonInteractorImpl;
import com.pyamsoft.padlock.model.sql.PadLockDB;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

final class LockInfoInteractorImpl extends LockCommonInteractorImpl implements LockInfoInteractor {

  @Inject public LockInfoInteractorImpl(final @NonNull Context context) {
    super(context);
  }

  @NonNull @Override
  public Observable<List<PadLockEntry>> getActivityEntries(@NonNull String packageName) {
    return PadLockDB.with(getAppContext())
        .createQuery(PadLockEntry.TABLE_NAME, PadLockEntry.WITH_PACKAGE_NAME, packageName)
        .mapToList(PadLockEntry.MAPPER::map);
  }
}
