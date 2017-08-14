/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.base.wrapper

import android.content.pm.ApplicationInfo
import android.support.annotation.CheckResult
import io.reactivex.Maybe
import io.reactivex.Single

interface PackageApplicationManager {

  @CheckResult fun getActiveApplications(): Single<List<ApplicationInfo>>

  @CheckResult fun getApplicationInfo(packageName: String): Maybe<ApplicationInfo>
}