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
import com.pyamsoft.padlock.dagger.TestPadLockComponent;
import javax.inject.Inject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestPadLock.class, sdk = 23, constants = BuildConfig.class)
public class MainPresenterTest {

  @Inject MainPresenter presenter;
  boolean usageTermsShown;

  @Before public void setup() {
    final TestPadLockComponent testPadLockComponent =
        PadLock.get(RuntimeEnvironment.application).provideComponent();
    testPadLockComponent.plusMain().inject(this);
  }

  @Test public void testing() {
    Assert.assertNotNull(presenter);

    usageTermsShown = false;
    presenter.bindView(new MainPresenter.MainView() {

      @Override public void showUsageTermsDialog() {
       usageTermsShown = true;
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {

      }
    });

    presenter.showTermsDialog();
    Assert.assertTrue(usageTermsShown);
  }
}
