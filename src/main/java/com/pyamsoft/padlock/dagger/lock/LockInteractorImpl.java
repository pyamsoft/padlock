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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import com.pyamsoft.padlock.app.lock.LockInteractor;
import timber.log.Timber;

public abstract class LockInteractorImpl implements LockInteractor {

  protected LockInteractorImpl() {

  }

  @WorkerThread @Override @NonNull
  public String appendToAttempt(final String attempt, final char numberCode) {
    if (isSubmittable(attempt)) {
      Timber.e("Attempt is all entered.");
      return attempt;
    }

    final int index = attempt.indexOf(DEFAULT_CHAR);
    return modifyAttempt(attempt, index, numberCode);
  }

  @WorkerThread @Override @NonNull public String deleteFromAttempt(final String attempt) {
    int workingIndex = attempt.indexOf(DEFAULT_CHAR);
    if (workingIndex == BAD_INDEX) {
      Timber.d("BACK at bad index, roll back to end");
      workingIndex = attempt.length();
    }

    // If the back key is pressed, move the active index back one and state the
    // character code as the default code again.
    if (workingIndex > 0) {
      workingIndex -= 1;
      Timber.d("Working index for delete: %d", workingIndex);
      return modifyAttempt(attempt, workingIndex, DEFAULT_CHAR);
    } else {
      // Exit if out of range
      Timber.d("Out of range, do not modify");
      return attempt;
    }
  }

  @WorkerThread @Override public boolean isSubmittable(final String attempt) {
    final int index = attempt.indexOf(DEFAULT_CHAR);
    Timber.d("Find index of DEFAULT_CHAR: %d", index);
    return index == BAD_INDEX;
  }

  @WorkerThread @NonNull
  private String modifyAttempt(final String attempt, final int modifyAt, final char modifyWith) {
    Timber.d("Modify string: %s with value %s at index %d", attempt, modifyWith, modifyAt);
    // String before current index exclusive, can be empty
    final String prefix = attempt.substring(0, modifyAt);

    // String after index exclusive, can be empty
    final String suffix = attempt.substring(modifyAt + 1, attempt.length());

    // Compile the new string and display it in the view
    return prefix + modifyWith + suffix;
  }

  @WorkerThread @Override
  public boolean compareAttemptToPIN(final String attempt, final String pin) {
    if (pin == null || attempt == null) {
      Timber.e("NULL passed");
      return false;
    } else {
      Timber.d("Compare \nhash: %s\nhash: %s", attempt, pin);
      final boolean equals = pin.equals(attempt);
      Timber.d("IS PIN? %s", equals);
      return equals;
    }
  }

  @NonNull @Override @WorkerThread
  public Drawable loadPackageIcon(Context context, final @NonNull String packageName) {
    final PackageManager packageManager = context.getApplicationContext().getPackageManager();
    Drawable image;
    try {
      image = packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager);
    } catch (PackageManager.NameNotFoundException e) {
      image = packageManager.getDefaultActivityIcon();
    }
    return image;
  }
}
