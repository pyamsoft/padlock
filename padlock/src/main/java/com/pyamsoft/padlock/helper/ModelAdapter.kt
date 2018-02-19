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

package com.pyamsoft.padlock.helper

import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.items.ModelAbstractItem

fun <M : Any> ModelAdapter<M, out ModelAbstractItem<M, *, *>>.retainAll(
    backing: MutableCollection<M>
): Boolean {
  val old: MutableCollection<ModelAbstractItem<M, *, *>> = LinkedHashSet()
  adapterItems.filterNotTo(old) { backing.contains(it.model) }

  old.asSequence()
      .map { getAdapterPosition(it.identifier) }
      .filter { it >= 0 }
      .forEach { remove(it) }

  backing.clear()
  return old.isNotEmpty()
}
