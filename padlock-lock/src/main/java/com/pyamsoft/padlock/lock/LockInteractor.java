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

package com.pyamsoft.padlock.lock;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.util.Base64;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import rx.Observable;

public class LockInteractor {

  @NonNull private static LockInteractor INSTANCE = new LockInteractor();
  @SuppressWarnings("WeakerAccess") @NonNull final MessageDigest messageDigest;

  LockInteractor() {
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Could not create SHA-256 Digest", e);
    }
  }

  public static void set(@NonNull LockInteractor interactor) {
    INSTANCE = interactor;
  }

  @CheckResult @NonNull public static LockInteractor get() {
    return INSTANCE;
  }

  @CheckResult @NonNull public Observable<String> encodeSHA256(@NonNull String attempt) {
    return Observable.fromCallable(() -> {
      messageDigest.reset();
      final byte[] output = messageDigest.digest(attempt.getBytes(Charset.defaultCharset()));
      return Base64.encodeToString(output, Base64.DEFAULT).trim();
    });
  }

  @NonNull @CheckResult public Observable<Boolean> checkSubmissionAttempt(@NonNull String attempt,
      @NonNull String encodedPin) {
    return encodeSHA256(attempt).map(encodedPin::equals);
  }
}
