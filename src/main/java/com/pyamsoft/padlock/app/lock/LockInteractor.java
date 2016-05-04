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
import android.support.annotation.WorkerThread;

public interface LockInteractor {

  char ZERO = '0';
  char ONE = '1';
  char TWO = '2';
  char THREE = '3';
  char FOUR = '4';
  char FIVE = '5';
  char SIX = '6';
  char SEVEN = '7';
  char EIGHT = '8';
  char NINE = '9';
  char DEFAULT_CHAR = '*';
  char BACK = DEFAULT_CHAR;
  String DEFAULT_STRING =
      new String(new char[] { DEFAULT_CHAR, DEFAULT_CHAR, DEFAULT_CHAR, DEFAULT_CHAR });
  int BAD_INDEX = -1;

  boolean isSubmittable(String attempt);

  @WorkerThread @NonNull String appendToAttempt(String attempt, char numberCode);

  @WorkerThread @NonNull String deleteFromAttempt(String attempt);

  @WorkerThread boolean compareAttemptToPIN(String attempt, String pin);

  @NonNull @WorkerThread Drawable loadPackageIcon(Context context,
      final @NonNull String packageName);
}
