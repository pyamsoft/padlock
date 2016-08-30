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

package com.pyamsoft.padlock.app.list;

import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.pyamsoft.padlock.model.ActivityEntry;
import com.pyamsoft.padlock.model.LockState;

class LockInfoAdapter extends FastItemAdapter<LockInfoItem>
    implements LockListDatabaseWhitelistView {

  @Override public void onDatabaseEntryCreated(int position) {
    final LockInfoItem oldItem = getItem(position);
    final LockInfoItem newItem = new LockInfoItem(oldItem.packageName,
        ActivityEntry.builder(oldItem.entry)
            .lockState(LockState.LOCKED)
            .build());
    set(position, newItem);
  }

  @Override public void onDatabaseEntryDeleted(int position) {
    final LockInfoItem oldItem = getItem(position);
    final LockInfoItem newItem = new LockInfoItem(oldItem.packageName,
        ActivityEntry.builder(oldItem.entry)
            .lockState(LockState.DEFAULT)
            .build());
    set(position, newItem);
  }

  @Override public void onDatabaseEntryWhitelisted(int position) {
    final LockInfoItem oldItem = getItem(position);
    final LockInfoItem newItem = new LockInfoItem(oldItem.packageName,
        ActivityEntry.builder(oldItem.entry)
            .lockState(LockState.WHITELISTED)
            .build());
    set(position, newItem);
  }
}
