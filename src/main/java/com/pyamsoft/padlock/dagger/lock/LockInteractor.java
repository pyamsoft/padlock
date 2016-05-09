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

package com.pyamsoft.padlock.dagger.lock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import rx.Observable;

public interface LockInteractor {

  @NonNull @WorkerThread Observable<Drawable> loadPackageIcon(Context context,
      final @NonNull String packageName);

  @WorkerThread @NonNull String encodeSHA256(@NonNull String attempt);

  @WorkerThread boolean checkSubmissionAttempt(@NonNull String attempt, @NonNull String encodedPin);

  @WorkerThread boolean checkEncodedSubmissionAttempt(@NonNull String encodedAttempt, @NonNull String encodedPin);
}
