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

package com.pyamsoft.padlock.dagger.main;

import com.pyamsoft.padlock.BuildConfig;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.TestPadLock;
import com.pyamsoft.padlock.app.main.MainPresenter;
import com.pyamsoft.padlock.bus.MainBus;
import com.pyamsoft.padlock.dagger.TestPadLockComponent;
import com.pyamsoft.padlock.model.event.RefreshEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Named;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.pyamsoft.padlock.TestUtils.log;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestPadLock.class, sdk = 23, constants = BuildConfig.class)
public class MainPresenterTest {

  @Inject @Named("agree") MainPresenterImpl agreePresenter;
  @Inject @Named("not_agree") MainPresenterImpl notAgreePresenter;

  @Before public void setup() {
    final TestPadLockComponent testPadLockComponent =
        PadLock.get(RuntimeEnvironment.application).provideComponent();
    testPadLockComponent.plusMain().inject(this);
  }

  /**
   * Make sure that the usage terms are shown when they are not agreed to
   */
  @Test public void testShowUsageTerms() throws Exception {
    assertNotNull(notAgreePresenter);
    assertFalse(notAgreePresenter.isBound());

    // KLUDGE Kind of ugly
    final AtomicBoolean usageTermsShown = new AtomicBoolean(false);
    notAgreePresenter.bindView(new MainPresenter.MainView() {

      @Override public void showUsageTermsDialog() {
        usageTermsShown.set(true);
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {

      }
    });

    assertFalse(usageTermsShown.get());
    notAgreePresenter.showTermsDialog();
    assertTrue(usageTermsShown.get());
  }

  /**
   * Make sure that any exceptions are swalloed when showing usage terms
   */
  @Test public void testSwallowExceptionUsageTerms() throws Exception {
    assertNotNull(notAgreePresenter);
    assertFalse(notAgreePresenter.isBound());

    notAgreePresenter.bindView(new MainPresenter.MainView() {
      @Override public void showUsageTermsDialog() {
        log("Throw exception, swallow with RX");
        throw new RuntimeException("Should not be here");
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {

      }
    });

    notAgreePresenter.showTermsDialog();
  }

  /**
   * Make sure refresh is handled
   */
  @Test public void testForceRefresh() throws Exception {
    assertNotNull(notAgreePresenter);
    assertFalse(notAgreePresenter.isBound());

    // KLUDGE Kind of ugly
    final AtomicBoolean forcedRefresh = new AtomicBoolean(false);
    notAgreePresenter.bindView(new MainPresenter.MainView() {
      @Override public void showUsageTermsDialog() {
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {
        forcedRefresh.set(true);
      }
    });

    assertFalse(forcedRefresh.get());
    MainBus.get().post(RefreshEvent.create());
    assertTrue(forcedRefresh.get());
  }

  /**
   * Make sure any exceptions are swallowed
   */
  @Test public void testSwallowExceptionForceRefresh() throws Exception {
    assertNotNull(notAgreePresenter);
    assertFalse(notAgreePresenter.isBound());

    notAgreePresenter.bindView(new MainPresenter.MainView() {
      @Override public void showUsageTermsDialog() {
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {
        log("Throw exception, swallow with RX");
        throw new RuntimeException("Should not be here");
      }
    });

    MainBus.get().post(RefreshEvent.create());
  }
}
