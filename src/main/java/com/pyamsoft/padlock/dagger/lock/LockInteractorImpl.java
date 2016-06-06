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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class LockInteractorImpl extends IconLoadInteractorImpl {

  @NonNull private final MessageDigest messageDigest;

  protected LockInteractorImpl(final @NonNull Context context) {
    super(context);
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not create SHA-256 Digest");
    }
  }

  @CheckResult @NonNull protected final String encodeSHA256(@NonNull String attempt) {
    messageDigest.reset();
    final byte[] output = messageDigest.digest(attempt.getBytes(Charset.defaultCharset()));
    return Base64.encodeToString(output, Base64.DEFAULT).trim();
  }

  @CheckResult @WorkerThread protected final boolean checkSubmissionAttempt(@NonNull String attempt,
      @NonNull String encodedPin) {
    final String encodedAttempt = encodeSHA256(attempt);
    return checkEncodedSubmissionAttempt(encodedAttempt, encodedPin);
  }

  @CheckResult @WorkerThread
  protected final boolean checkEncodedSubmissionAttempt(@NonNull String encodedAttempt,
      @NonNull String encodedPin) {
    return encodedPin.equals(encodedAttempt);
  }
}
