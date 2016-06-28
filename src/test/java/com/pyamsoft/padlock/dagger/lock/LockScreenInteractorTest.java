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
import com.pyamsoft.padlock.app.sql.PadLockDB;
import com.pyamsoft.padlock.dagger.base.PackageManagerWrapperImpl;
import com.pyamsoft.padlock.dagger.db.DBInteractor;
import com.pyamsoft.padlock.model.sql.PadLockEntry;
import com.pyamsoft.padlock.model.sql.TestPadLockEntry;
import java.util.Collections;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, application = PadLock.class)
public class LockScreenInteractorTest {

  private Context context;
  private DBInteractor mockDBInteractor;
  private PadLockPreferences mockPreferences;
  private PackageManagerWrapperImpl packageManagerWrapper;
  private MasterPinInteractor mockMasterPinInteractor;

  void reinit() {
    PadLockDB.setDelegate(null);
    context = RuntimeEnvironment.application.getApplicationContext();
    mockDBInteractor = Mockito.mock(DBInteractor.class);
    mockPreferences = Mockito.mock(PadLockPreferences.class);
    packageManagerWrapper = new PackageManagerWrapperImpl(context);
    mockMasterPinInteractor = Mockito.mock(MasterPinInteractor.class);
  }

  @CheckResult @NonNull public LockScreenInteractor getLockScreenInteractor() {
    return new LockScreenInteractorImpl(context, mockPreferences, mockDBInteractor,
        mockMasterPinInteractor, packageManagerWrapper);
  }

  @Test public void test_ignoreTime() {
    reinit();
    final LockScreenInteractor interactor = getLockScreenInteractor();
    Assert.assertEquals(0L, interactor.getDefaultIgnoreTime().toBlocking().first().longValue());
  }

  @Test public void test_displayName() {
    reinit();
    final LockScreenInteractor interactor = getLockScreenInteractor();

    // Robolectric returns the packageName
    String packageName = "com.pyamsoft.padlock";
    TestSubscriber<String> testSubscriber = new TestSubscriber<>();
    interactor.getDisplayName(packageName).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertReceivedOnNext(Collections.singletonList(packageName + ".PadLock"));
  }

  void init_test_unlock(boolean testingResponse) {
    reinit();
    // Create a fake delegate
    final PadLockDB.Delegate delegate = new PadLockDB.Delegate(context, Schedulers.immediate()) {
      @NonNull @Override
      public Observable<PadLockEntry> queryWithPackageActivityName(@NonNull String packageName,
          @NonNull String activityName) {
        return Observable.defer(() -> Observable.create(new Observable.OnSubscribe<PadLockEntry>() {
          @Override public void call(Subscriber<? super PadLockEntry> subscriber) {
            //System.out.println(
            //    "FAKE queryWithPackageActivityName(  _packageName_ , _activityName_ )");
            subscriber.onStart();
            subscriber.onNext(testingResponse ? TestPadLockEntry.test() : TestPadLockEntry.empty());
            subscriber.onCompleted();
          }
        }));
      }
    };
    PadLockDB.setDelegate(delegate);

    // Mock preference
    Mockito.when(mockPreferences.getIgnoreTimes())
        .thenReturn(new long[] { 0, 1, 5, 10, 15, 20, 30, 45, 60 });
  }

  @Test public void test_unlockEmptyFail() {
    String packageName = "com.pyamsoft.padlock";
    String activityName = packageName + ".app.main.MainActivity";

    init_test_unlock(false);
    final LockScreenInteractor interactor = getLockScreenInteractor();

    // Mock master pin methods
    Mockito.when(mockMasterPinInteractor.getMasterPin())
        .thenReturn(Observable.create(new Observable.OnSubscribe<String>() {
          @Override public void call(Subscriber<? super String> subscriber) {
            //System.out.println("MOCK getMasterPin()");
            subscriber.onStart();
            subscriber.onNext(interactor.encodeSHA256("BOB").toBlocking().first());
            subscriber.onCompleted();
          }
        }));

    final TestSubscriber<Boolean> testSubscriber = TestSubscriber.create();
    interactor.unlockEntry(packageName, activityName, "BOB").subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValue(false);
  }

  @Test public void test_unlockFail() {
    String packageName = "com.pyamsoft.padlock";
    String activityName = packageName + ".app.main.MainActivity";

    init_test_unlock(true);
    final LockScreenInteractor interactor = getLockScreenInteractor();

    // Mock master pin methods
    Mockito.when(mockMasterPinInteractor.getMasterPin())
        .thenReturn(Observable.create(new Observable.OnSubscribe<String>() {
          @Override public void call(Subscriber<? super String> subscriber) {
            //System.out.println("MOCK getMasterPin()");
            subscriber.onStart();
            subscriber.onNext(interactor.encodeSHA256("BOB").toBlocking().first());
            subscriber.onCompleted();
          }
        }));

    final TestSubscriber<Boolean> testSubscriber = TestSubscriber.create();
    interactor.unlockEntry(packageName, activityName, "").subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValue(false);
  }

  @Test public void test_unlockSuccess() {
    String packageName = "com.pyamsoft.padlock";
    String activityName = packageName + ".app.main.MainActivity";

    init_test_unlock(true);
    final LockScreenInteractor interactor = getLockScreenInteractor();

    // Mock master pin methods
    Mockito.when(mockMasterPinInteractor.getMasterPin())
        .thenReturn(Observable.create(new Observable.OnSubscribe<String>() {
          @Override public void call(Subscriber<? super String> subscriber) {
            //System.out.println("MOCK getMasterPin()");
            subscriber.onStart();
            subscriber.onNext(interactor.encodeSHA256("BOB").toBlocking().first());
            subscriber.onCompleted();
          }
        }));

    final TestSubscriber<Boolean> testSubscriber = TestSubscriber.create();
    interactor.unlockEntry(packageName, activityName, "BOB").subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValue(true);
  }
}
