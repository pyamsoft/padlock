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

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public final class LockListLayoutManager extends LinearLayoutManager {

  private boolean verticalScrollEnabled = true;

  public LockListLayoutManager(Context context) {
    super(context);
  }

  @Override public boolean canScrollVertically() {
    return verticalScrollEnabled && super.canScrollVertically();
  }

  public boolean isVerticalScrollEnabled() {
    return verticalScrollEnabled;
  }

  public void setVerticalScrollEnabled(boolean verticalScrollEnabled) {
    this.verticalScrollEnabled = verticalScrollEnabled;
  }
}
