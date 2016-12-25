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

package com.pyamsoft.presenter.main;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import static com.pyamsoft.padlock.TestUtils.log;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MainPresenterTest {

  @Mock MainInteractor interactor;
  private MainPresenterImpl presenter;

  @Before public void setup() {
    interactor = Mockito.mock(MainInteractor.class);
    presenter = new MainPresenterImpl(interactor, Schedulers.immediate(), Schedulers.immediate());
  }

  /**
   * Make sure that the usage terms are shown when they are not agreed to
   */
  @Test public void testShowUsageTerms() throws Exception {
    Mockito.when(interactor.hasAgreed()).then(invocation -> Observable.just(false));

    assertNotNull(presenter);
    assertFalse(presenter.isBound());

    // KLUDGE Kind of ugly
    final AtomicBoolean usageTermsShown = new AtomicBoolean(false);
    presenter.bindView(new MainPresenter.MainView() {

      @Override public void showUsageTermsDialog() {
        usageTermsShown.set(true);
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {

      }
    });

    assertFalse(usageTermsShown.get());
    presenter.showTermsDialog();
    assertTrue(usageTermsShown.get());
  }

  /**
   * Make sure that any exceptions are swalloed when showing usage terms
   */
  @Test public void testSwallowExceptionUsageTerms() throws Exception {
    Mockito.when(interactor.hasAgreed()).then(invocation -> Observable.just(true));
    assertNotNull(presenter);
    assertFalse(presenter.isBound());

    presenter.bindView(new MainPresenter.MainView() {
      @Override public void showUsageTermsDialog() {
        log("Throw exception, swallow with RX");
        throw new RuntimeException("Should not be here");
      }

      @Override public void onDidNotAgreeToTerms() {

      }

      @Override public void forceRefresh() {

      }
    });

    presenter.showTermsDialog();
  }
}
