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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.app.lock.LockScreen;
import com.pyamsoft.padlock.app.lock.LockScreenPresenter;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.schedulers.Schedulers;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = PadLock.class)
public class LockScreenPresenterTest {

  @Mock LockScreen mockLockScreen = Mockito.mock(LockScreen.class);
  @Mock LockScreenInteractor mockInteractor;
  LockScreenPresenter presenter;

  @CheckResult @NonNull public static LockScreenPresenter getLockScreenPresenter(
      final LockScreenInteractor mockInteractor) {
    return new LockScreenPresenter(mockInteractor, Schedulers.immediate(),
        Schedulers.immediate(), 0, 5, 10, 30);
  }

  @Test public void test_ignoreTimes() {
    mockInteractor = Mockito.mock(LockScreenInteractor.class);
    presenter = getLockScreenPresenter(mockInteractor);

    Assert.assertEquals(0, presenter.getIgnoreTimeNone());
    Assert.assertEquals(5, presenter.getIgnoreTimeFive());
    Assert.assertEquals(10, presenter.getIgnoreTimeTen());
    Assert.assertEquals(30, presenter.getIgnoreTimeThirty());
  }

  public void run_test_loadDisplayName(String packageName, String displayName) {
    Mockito.when(mockInteractor.getDisplayName(packageName))
        .thenReturn(new Observable<String>(subscriber -> {
          subscriber.onStart();
          subscriber.onNext(displayName);
          subscriber.onCompleted();
        }) {
        });

    // Set up the lock screen
    presenter.bindView(mockLockScreen);

    // Mock the return value
    Mockito.doAnswer(invocation -> {
      Assert.assertNotNull(invocation.getArguments());
      Assert.assertEquals(1, invocation.getArguments().length);
      Assert.assertEquals(displayName, invocation.getArguments()[0]);
      return null;
    }).when(mockLockScreen).setDisplayName(displayName);

    // Actual test
    presenter.loadDisplayNameFromPackage(packageName);

    // Cleanup
    presenter.unbindView();
  }

  @Test public void test_loadDisplayName() {
    mockInteractor = Mockito.mock(LockScreenInteractor.class);
    presenter = getLockScreenPresenter(mockInteractor);

    String packageName = "com.pyamsoft.padlock";
    String displayName = "PadLock";
    run_test_loadDisplayName(packageName, displayName);

    packageName = "com.test.example";
    displayName = "Example";
    run_test_loadDisplayName(packageName, displayName);
  }
}
