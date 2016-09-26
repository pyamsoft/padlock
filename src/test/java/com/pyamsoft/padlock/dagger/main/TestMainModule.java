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

import com.pyamsoft.padlock.app.main.MainPresenter;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;

@Module public class TestMainModule {

  @Provides MainPresenter provideMainPresenter() {
    throw new RuntimeException("Use Impl class to test");
  }

  @Named("agree") @Provides MainPresenterImpl provideMainPresenterAgree(
      @Named("agree") MainInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    return new MainPresenterImpl(interactor, obsScheduler, subScheduler);
  }

  @Named("not_agree") @Provides MainPresenterImpl provideMainPresenterNotAgree(
      @Named("not_agree") MainInteractor interactor, @Named("obs") Scheduler obsScheduler,
      @Named("sub") Scheduler subScheduler) {
    return new MainPresenterImpl(interactor, obsScheduler, subScheduler);
  }

  @Named("not_agree") @Provides MainInteractor provideMainInteractorNotAgree() {
    final MainInteractor interactor = Mockito.mock(MainInteractor.class);
    Mockito.when(interactor.hasAgreed()).thenReturn(Observable.just(false));
    Mockito.doAnswer(invocation -> {
      System.out.println("AGREE TERMS");
      return null;
    }).when(interactor).setAgreed();
    return interactor;
  }

  @Named("agree") @Provides MainInteractor provideMainInteractorAgree() {
    final MainInteractor interactor = Mockito.mock(MainInteractor.class);
    Mockito.when(interactor.hasAgreed()).thenReturn(Observable.just(true));
    Mockito.doAnswer(invocation -> {
      System.out.println("AGREE TERMS");
      return null;
    }).when(interactor).setAgreed();
    return interactor;
  }
}
