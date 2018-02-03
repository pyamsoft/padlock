/*
 *     Copyright (C) 2017 Peter Kenji Yamanaka
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.pyamsoft.padlock.lock.helper

import android.util.Base64
import com.pyamsoft.padlock.api.LockHelper
import io.reactivex.Completable
import io.reactivex.Single
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SHA256LockHelper @Inject internal constructor() :
    LockHelper {

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
    return Completable.fromAction({ messageDigest.reset() })
        .andThen(Single.fromCallable {
          messageDigest.digest(attempt.toByteArray(Charset.defaultCharset()))
        })
        .map {
          Base64.encodeToString(it, Base64.DEFAULT)
              .trim()
        }
  }
}
