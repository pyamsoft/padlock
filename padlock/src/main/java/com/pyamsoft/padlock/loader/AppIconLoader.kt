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

package com.pyamsoft.padlock.loader

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import com.pyamsoft.padlock.R
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loader

@CheckResult
fun ImageLoader.loadAppIcon(packageName: String, @DrawableRes icon: Int): Loader<Drawable> {
  return AppIconImageLoader(packageName, icon)
}

@CheckResult
fun ImageLoader.loadPadLockIcon(context: Context): Loader<Drawable> {
  return AppIconImageLoader(context.packageName, R.mipmap.ic_launcher)
}
