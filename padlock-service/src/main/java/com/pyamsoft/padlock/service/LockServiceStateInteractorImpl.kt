/*
 * Copyright (C) 2018 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlock.service

import com.pyamsoft.padlock.api.LockServiceStateInteractor
import com.pyamsoft.padlock.api.MasterPinInteractor
import com.pyamsoft.pydroid.optional.Optional.Present
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockServiceStateInteractorImpl @Inject internal constructor(
  private val pinInteractor: MasterPinInteractor
) : LockServiceStateInteractor {

  override fun isServiceEnabled(): Single<Boolean> =
    pinInteractor.getMasterPin().map { it is Present }
}
