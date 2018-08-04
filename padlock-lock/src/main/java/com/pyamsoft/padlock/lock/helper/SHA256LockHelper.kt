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

package com.pyamsoft.padlock.lock.helper

import android.util.Base64
import com.pyamsoft.padlock.api.lockscreen.LockHelper
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Single
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SHA256LockHelper @Inject internal constructor(
  private val enforcer: Enforcer
) : LockHelper {

  private val messageDigest: MessageDigest

  init {
    try {
      messageDigest = MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException("Could not create SHA-256 Digest", e)
    }
  }

  override fun checkSubmissionAttempt(
    attempt: String,
    encodedPin: String
  ): Single<Boolean> =
    encode(attempt).map { it == encodedPin }

  override fun encode(attempt: String): Single<String> {
    return Completable.fromAction {
      enforcer.assertNotOnMainThread()
      messageDigest.reset()
    }
        .andThen(Single.fromCallable {
          enforcer.assertNotOnMainThread()
          return@fromCallable messageDigest.digest(attempt.toByteArray(Charset.defaultCharset()))
        })
        .map {
          Base64.encodeToString(it, Base64.DEFAULT)
              .trim()
        }
  }
}
