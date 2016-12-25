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

package com.pyamsoft.padlock.presenter.lock;

import com.pyamsoft.padlock.presenter.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 23, constants = BuildConfig.class)
public class LockInteractorTest {

  private LockInteractor interactor;

  @Before public void setup() {
    interactor = new LockInteractorImpl() {
    };
  }

  /**
   * Test that the same string encoded twice produces the same result
   *
   * @throws Exception
   */
  @Test public void testEncodeIndentity() throws Exception {
    final String encoded = interactor.encodeSHA256("TEST 1").toBlocking().first();
    assertEquals(encoded, interactor.encodeSHA256("TEST 1").toBlocking().first());
    assertTrue(interactor.checkSubmissionAttempt("TEST 1", encoded).toBlocking().first());
  }

  /**
   * Test that two different strings produces different results
   *
   * @throws Exception
   */
  @Test public void testEncodeCorrectness() throws Exception {
    final String encoded = interactor.encodeSHA256("TEST 1").toBlocking().first();
    assertNotEquals(encoded, interactor.encodeSHA256("RANDOM").toBlocking().first());
    assertFalse(interactor.checkSubmissionAttempt("ANOTHER RANDOM", encoded).toBlocking().first());
  }
}
