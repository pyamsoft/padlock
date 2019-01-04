/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.padlock.loader

import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import com.pyamsoft.padlock.api.packagemanager.PackageActivityManager
import com.pyamsoft.pydroid.loader.Loader
import javax.inject.Inject
import javax.inject.Singleton

class AppIconLoader @Inject internal constructor(
  private val packageActivityManager: PackageActivityManager
) {

  @CheckResult
  fun loadAppIcon(packageName: String, @DrawableRes icon: Int): Loader<Drawable> {
    return AppIconImageLoader(packageName, icon, packageActivityManager)
  }

}

