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

package com.pyamsoft.padlock.pin;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.pyamsoft.padlock.list.LockListFragment;
import com.pyamsoft.padlock.lock.master.MasterPinSubmitCallback;
import com.pyamsoft.pydroid.ActionSingle;

abstract class PinEntryBaseFragment extends Fragment {

  void dismissParent() {
    final FragmentManager fragmentManager = getParentFragment().getFragmentManager();
    final Fragment pinFragment = fragmentManager.findFragmentByTag(PinEntryDialog.TAG);
    if (pinFragment instanceof PinEntryDialog) {
      ((PinEntryDialog) pinFragment).dismiss();
    } else {
      throw new ClassCastException("Fragment is not PinEntryDialog");
    }
  }

  void actOnLockList(@NonNull ActionSingle<MasterPinSubmitCallback> action) {
    final FragmentManager fragmentManager = getParentFragment().getFragmentManager();
    final Fragment lockListFragment = fragmentManager.findFragmentByTag(LockListFragment.TAG);
    if (lockListFragment instanceof LockListFragment) {
      ((LockListFragment) lockListFragment).provideMasterSubmitCallback(action);
    } else {
      throw new ClassCastException("Fragment is not MasterPinSubmitCallback");
    }
  }
}
