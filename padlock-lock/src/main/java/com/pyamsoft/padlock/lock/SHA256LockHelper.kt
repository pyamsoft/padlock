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

package com.pyamsoft.padlock.lock

import android.support.annotation.CheckResult
import android.util.Base64
import io.reactivex.Completable
import io.reactivex.Single
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SHA256LockHelper private constructor() : LockHelper() {

  private val messageDigest: MessageDigest

  init {
    try {
      messageDigest = MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException("Could not create SHA-256 Digest", e)
    }

  }

  override fun encodeSHA256(attempt: String): Single<String> {
    return Completable.fromAction({ messageDigest.reset() })
        .andThen(Single.fromCallable {
          messageDigest.digest(attempt.toByteArray(Charset.defaultCharset()))
        }).map { Base64.encodeToString(it, Base64.DEFAULT).trim() }
  }

  companion object {

    @JvmStatic
    @CheckResult fun newInstance(): SHA256LockHelper {
      return SHA256LockHelper()
    }
  }
}
