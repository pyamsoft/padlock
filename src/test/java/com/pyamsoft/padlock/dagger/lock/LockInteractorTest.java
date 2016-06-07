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

import com.pyamsoft.padlock.BuildConfig;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * We must run with robo electric because Base64 is in the Android classes
 */
@RunWith(RobolectricGradleTestRunner.class) @Config(constants = BuildConfig.class)
public class LockInteractorTest {

  @Test public void test_sha256Encode() {
    final LockInteractor interactor = new LockInteractorImpl() {

    };

    String attempt = "";
    String encoded = interactor.encodeSHA256(attempt);
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded));

    attempt = "test";
    encoded = interactor.encodeSHA256(attempt);
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded));

    attempt = "asd9u0290 112-012 -10";
    encoded = interactor.encodeSHA256(attempt);
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded));

    attempt = "\n\tasdiads$";
    encoded = interactor.encodeSHA256(attempt);
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded));

    attempt = "平仮名";
    encoded = interactor.encodeSHA256(attempt);
    Assert.assertNotNull(encoded);
    Assert.assertTrue(interactor.checkSubmissionAttempt(attempt, encoded));
  }
}
