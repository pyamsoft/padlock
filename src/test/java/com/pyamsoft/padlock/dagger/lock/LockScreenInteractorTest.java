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
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.PadLockPreferences;
import com.pyamsoft.padlock.dagger.db.DBInteractor;
import java.util.Collections;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.observers.TestSubscriber;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = PadLock.class)
public class LockScreenInteractorTest {

  @CheckResult @NonNull public static LockScreenInteractor getLockScreenInteractor() {
    final Context context = RuntimeEnvironment.application.getApplicationContext();
    final DBInteractor mockDBInteractor = Mockito.mock(DBInteractor.class);
    final PadLockPreferences mockPreferences = Mockito.mock(PadLockPreferences.class);
    final MasterPinInteractor mockMasterPinInteractor = Mockito.mock(MasterPinInteractor.class);
    return new LockScreenInteractorImpl(context, mockPreferences, mockDBInteractor,
        mockMasterPinInteractor, packageManagerWrapper);
  }

  @Test public void test_ignoreTime() {
    final LockScreenInteractor interactor = getLockScreenInteractor();
    Assert.assertEquals(0L, interactor.getDefaultIgnoreTime().toBlocking().first().longValue());
  }

  @Test public void test_diaplyName() {
    final LockScreenInteractor interactor = getLockScreenInteractor();

    // Robolectric returns the packageName
    String packageName = "com.pyamsoft.padlock";
    TestSubscriber<String> testSubscriber = new TestSubscriber<>();
    interactor.getDisplayName(packageName).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertReceivedOnNext(Collections.singletonList(packageName + ".PadLock"));
  }
}
