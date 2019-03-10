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

package com.pyamsoft.padlock.list.info

import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.padlock.list.FilterableItem
import com.pyamsoft.padlock.model.list.ActivityEntry

abstract class LockInfoBaseItem<
    M : ActivityEntry,
    I : LockInfoBaseItem<M, I, VH>,
    VH : RecyclerView.ViewHolder>
protected constructor(entry: M) : ModelAbstractItem<M, I, VH>(entry),
    FilterableItem<I, VH>
