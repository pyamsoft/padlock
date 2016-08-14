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

import android.support.annotation.NonNull;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * We must run with robo electric because Base64 is in the Android classes
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = PadLock.class)
public class LockInteractorTest {

  private LockInteractorImpl interactor;

  private void run_test_sha256Encode(@NonNull String attempt) {
    final String encoded = interactor.encodeSHA256(attempt).toBlocking().first();
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded).toBlocking().first());
  }

  @Test public void test_sha256Encode() {
    interactor = new LockInteractorImpl() {

    };

    run_test_sha256Encode("");
    run_test_sha256Encode("test");
    run_test_sha256Encode("asd9u0290 112-012 -10");
    run_test_sha256Encode("\n\tasdiads$");
    run_test_sha256Encode("平仮名");
  }
}
