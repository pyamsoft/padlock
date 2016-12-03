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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DividerItemDecoration;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.databinding.FragmentApplistBinding;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.app.ListAdapterLoader;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.PersistentCache;
import com.pyamsoft.pydroiddesign.fab.HideScrollFABBehavior;
import com.pyamsoft.pydroiddesign.util.FABUtil;
import com.pyamsoft.pydroidrx.RXLoader;
import timber.log.Timber;

public class LockListFragment extends ActionBarFragment
    implements LockListPresenter.LockList, PinEntryDialogRequest {

  @NonNull public static final String TAG = "LockListFragment";
  @NonNull private static final String PIN_DIALOG_TAG = "pin_dialog";
  @NonNull private static final String KEY_LOAD_ADAPTER = "key_load_adapter";
  @NonNull private static final String KEY_PRESENTER = "key_presenter";
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @NonNull private final AsyncDrawable.Mapper taskMap = new AsyncDrawable.Mapper();
  @SuppressWarnings("WeakerAccess") LockListAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") LockListLayoutManager lockListLayoutManager;
  @SuppressWarnings("WeakerAccess") LockListPresenter presenter;
  @SuppressWarnings("WeakerAccess") FragmentApplistBinding binding;
  @NonNull private final Runnable startRefreshRunnable = new Runnable() {
    @Override public void run() {
      binding.applistSwipeRefresh.setRefreshing(true);
      lockListLayoutManager.setVerticalScrollEnabled(false);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    }
  };
  @NonNull private final Runnable stopRefreshRunnable = new Runnable() {
    @Override public void run() {
      binding.applistSwipeRefresh.setRefreshing(false);
      lockListLayoutManager.setVerticalScrollEnabled(true);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    }
  };
  @SuppressWarnings("WeakerAccess") boolean forceRefresh;
  private MenuItem displaySystemItem;
  private long loadedPresenterKey;
  private long loadedAdapterKey;
  @Nullable private TapTargetSequence sequence;
  private DividerItemDecoration dividerDecoration;

  @CheckResult @NonNull public static LockListFragment newInstance(boolean forceRefresh) {
    final LockListFragment fragment = new LockListFragment();
    final Bundle args = new Bundle();
    args.putBoolean("FORCE_REFRESH", forceRefresh);
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    loadedPresenterKey = PersistentCache.get()
        .load(KEY_PRESENTER, savedInstanceState, new PersistLoader.Callback<LockListPresenter>() {
          @NonNull @Override public PersistLoader<LockListPresenter> createLoader() {
            forceRefresh = true;
            return new LockListPresenterLoader();
          }

          @Override public void onPersistentLoaded(@NonNull LockListPresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.get()
        .load(KEY_LOAD_ADAPTER, savedInstanceState, new PersistLoader.Callback<LockListAdapter>() {
          @NonNull @Override public PersistLoader<LockListAdapter> createLoader() {
            return new ListAdapterLoader<LockListAdapter>() {
              @NonNull @Override public LockListAdapter loadPersistent() {
                return new LockListAdapter();
              }
            };
          }

          @Override public void onPersistentLoaded(@NonNull LockListAdapter persist) {
            fastItemAdapter = persist;
          }
        });
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_applist, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final boolean forceRefresh = getArguments().getBoolean("FORCE_REFRESH", false);
    if (forceRefresh) {
      Timber.d("Force a list refresh");
      this.forceRefresh = true;
    }

    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();
  }

  @Override public void onStart() {
    super.onStart();
    binding.applistRecyclerview.setAdapter(fastItemAdapter);
    presenter.bindView(this);

    presenter.setFABStateFromPreference();
    if (forceRefresh) {
      Timber.d("Do initial refresh");
      forceRefresh = false;
      refreshList();
    } else {
      Timber.d("We are already refreshed, just refresh the request listeners");
      applyUpdatedRequestListeners();
      presenter.showOnBoarding();
    }
  }

  private void applyUpdatedRequestListeners() {
    for (final LockListItem item : fastItemAdapter.getAdapterItems()) {
      item.setRequestListener(this::displayLockInfoFragment);
      item.setModifyListener(this::processDatabaseModifyEvent);
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> binding.applistFab.show(), 300L);

    MainActivity.getNavigationDrawerController(getActivity()).drawerNormalNavigation();
    setActionBarUpEnabled(true);
    getActivity().supportInvalidateOptionsMenu();
  }

  @Override public void onPause() {
    super.onPause();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> binding.applistFab.hide(), 300L);
  }

  private void setupSwipeRefresh() {
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.applistSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    dividerDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);

    binding.applistRecyclerview.setLayoutManager(lockListLayoutManager);
    binding.applistRecyclerview.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (isResumed()) {
      setupLockListMenuItems(menu);
    }
  }

  private void setupLockListMenuItems(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    presenter.setSystemVisibilityFromPreference();
  }

  private void setSystemCheckListener() {
    displaySystemItem.setOnMenuItemClickListener(item -> {
      if (binding.applistSwipeRefresh != null && !binding.applistSwipeRefresh.isRefreshing()) {
        Timber.d("List is not refreshing. Allow change of system preference");
        if (item.isChecked()) {
          presenter.setSystemInvisible();
        } else {
          presenter.setSystemVisible();
        }

        refreshList();
      }
      return true;
    });
  }

  @SuppressWarnings("WeakerAccess") void setSystemVisible(boolean visible) {
    displaySystemItem.setOnMenuItemClickListener(null);
    displaySystemItem.setChecked(visible);
    setSystemCheckListener();
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedPresenterKey);
    PersistentCache.get().saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
    super.onSaveInstanceState(outState);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    clearListListeners();
    binding.applistRecyclerview.removeItemDecoration(dividerDecoration);
    binding.applistRecyclerview.setOnClickListener(null);
    binding.applistRecyclerview.setLayoutManager(null);
    binding.applistRecyclerview.setAdapter(null);

    binding.applistFab.setOnClickListener(null);
    binding.applistSwipeRefresh.setOnRefreshListener(null);
    taskMap.clear();
    handler.removeCallbacksAndMessages(null);
    binding.unbind();
  }

  private void clearListListeners() {
    final int oldSize = fastItemAdapter.getAdapterItems().size() - 1;
    if (oldSize <= 0) {
      Timber.w("List is already empty");
      return;
    }

    for (int i = oldSize; i >= 0; --i) {
      final LockListItem item = fastItemAdapter.getAdapterItem(i);
      if (item != null) {
        item.cleanup();
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.get().unload(loadedPresenterKey);
      PersistentCache.get().unload(loadedAdapterKey);
    }
  }

  private void setupFAB() {
    binding.applistFab.setOnClickListener(view -> presenter.clickPinFAB());
    FABUtil.setupFABBehavior(binding.applistFab, new HideScrollFABBehavior(24));
  }

  @Override public void setFABStateEnabled() {
    final AsyncMap.Entry fabIconTask = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_lock_outline_24dp, new RXLoader())
        .into(binding.applistFab);
    taskMap.put("fab", fabIconTask);
  }

  @Override public void setFABStateDisabled() {
    final AsyncMap.Entry fabIconTask = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_lock_open_24dp, new RXLoader())
        .into(binding.applistFab);
    taskMap.put("fab", fabIconTask);
  }

  @Override public void onCreateAccessibilityDialog() {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new AccessibilityRequestDialog(),
        "accessibility");
  }

  @Override public void onCreatePinDialog() {
    onPinEntryDialogRequested(getContext().getPackageName(), getActivity().getClass().getName());
  }

  @Override
  public void onPinEntryDialogRequested(@NonNull String packageName, @NonNull String activityName) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(),
        PinEntryDialog.newInstance(packageName, activityName), PIN_DIALOG_TAG);
  }

  @Override public void onCreateMasterPinSuccess() {
    setFABStateEnabled();
    final View v = getView();
    if (v != null) {
      Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override public void onCreateMasterPinFailure() {
    Toast.makeText(getContext(), "Error: Mismatched PIN", Toast.LENGTH_SHORT).show();
  }

  @Override public void onClearMasterPinSuccess() {
    setFABStateDisabled();
    final View v = getView();
    if (v != null) {
      Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override public void onClearMasterPinFailure() {
    Toast.makeText(getContext(), "Error: Invalid PIN", Toast.LENGTH_SHORT).show();
  }

  @Override public void onEntryAddedToList(@NonNull AppEntry entry) {
    Timber.d("Add entry: %s", entry);

    // In case the configuration changes, we do the animation again
    if (!binding.applistSwipeRefresh.isRefreshing()) {
      binding.applistSwipeRefresh.setRefreshing(true);
    }

    fastItemAdapter.add(
        new LockListItem(entry, this::displayLockInfoFragment, this::processDatabaseModifyEvent));
    fastItemAdapter.notifyAdapterItemInserted(fastItemAdapter.getItemCount() - 1);
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    if (sequence == null) {
      final TapTarget fabTarget =
          TapTarget.forView(binding.applistFab, getString(R.string.getting_started),
              getString(R.string.getting_started_desc)).cancelable(false).tintTarget(false);

      // If we use the first item we get a weird location, try a different item
      TapTarget listTarget = null;
      final LockListItem.ViewHolder holder =
          (LockListItem.ViewHolder) binding.applistRecyclerview.findViewHolderForAdapterPosition(1);
      if (holder != null) {
        final View switchView = holder.binding.lockListToggle;
        listTarget = TapTarget.forView(switchView, getString(R.string.onboard_title_locklist),
            getString(R.string.onboard_desc_locklist)).tintTarget(false).cancelable(false);
      }

      // Hold a ref to the sequence or Activity will recycle bitmaps and crash
      sequence = new TapTargetSequence(getActivity());
      if (fabTarget != null) {
        sequence.target(fabTarget);
      }
      if (listTarget != null) {
        sequence.target(listTarget);
      }

      sequence.listener(new TapTargetSequence.Listener() {
        @Override public void onSequenceFinish() {
          if (presenter != null) {
            presenter.setOnBoard();
          }
        }

        @Override public void onSequenceCanceled(TapTarget lastTarget) {

        }
      });
    }

    sequence.start();
  }

  @Override public void onListCleared() {
    Timber.d("Prepare for refresh");
    forceRefresh = true;

    Timber.d("onListCleared");
    handler.post(startRefreshRunnable);
    handler.post(() -> binding.applistFab.hide());
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    fastItemAdapter.notifyAdapterDataSetChanged();
    handler.post(stopRefreshRunnable);
    handler.post(() -> binding.applistFab.show());

    Timber.d("We have refreshed");
    forceRefresh = false;

    presenter.showOnBoarding();
  }

  void clearList() {
    final int oldSize = fastItemAdapter.getAdapterItems().size() - 1;
    if (oldSize <= 0) {
      Timber.w("List is already empty");
      return;
    }

    for (int i = oldSize; i >= 0; --i) {
      final LockListItem item = fastItemAdapter.getAdapterItem(i);
      if (item != null) {
        item.cleanup();
      }
      fastItemAdapter.remove(i);
    }
  }

  @Override public void refreshList() {
    clearList();

    onListCleared();
    presenter.populateList();
  }

  @Override public void onDatabaseEntryCreated(int position) {
    fastItemAdapter.onDatabaseEntryCreated(position);
  }

  @Override public void onDatabaseEntryDeleted(int position) {
    fastItemAdapter.onDatabaseEntryDeleted(position);
  }

  @Override public void onDatabaseEntryError(int position) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  void processDatabaseModifyEvent(boolean lock, int position, @NonNull AppEntry entry) {
    Timber.d("Received a database modify event request for %s at %d [%s]", entry.packageName(),
        position, lock ? "LOCK" : "NO LOCK");
    presenter.modifyDatabaseEntry(lock, position, entry.packageName(), null, entry.system());
  }

  void displayLockInfoFragment(@NonNull AppEntry entry) {
    final FragmentManager fragmentManager = getFragmentManager();
    if (fragmentManager.findFragmentByTag(LockInfoFragment.TAG) == null) {
      fragmentManager.beginTransaction()
          .add(R.id.main_view_container, LockInfoFragment.newInstance(entry), LockInfoFragment.TAG)
          .addToBackStack(null)
          .commit();
    }
  }
}
