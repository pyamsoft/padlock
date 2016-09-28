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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.app.ListAdapterLoader;
import com.pyamsoft.pydroid.app.PersistLoader;
import com.pyamsoft.pydroid.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.behavior.HideScrollFABBehavior;
import com.pyamsoft.pydroid.tool.AsyncMap;
import com.pyamsoft.pydroid.util.AppUtil;
import com.pyamsoft.pydroid.util.AsyncDrawable;
import com.pyamsoft.pydroid.util.PersistentCache;
import com.pyamsoft.pydroid.widget.DividerItemDecoration;
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
  @BindView(R.id.applist_fab) FloatingActionButton fab;
  @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @SuppressWarnings("WeakerAccess") LockListAdapter fastItemAdapter;
  @SuppressWarnings("WeakerAccess") LockListLayoutManager lockListLayoutManager;
  @NonNull private final Runnable startRefreshRunnable = new Runnable() {
    @Override public void run() {
      swipeRefreshLayout.setRefreshing(true);
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
      swipeRefreshLayout.setRefreshing(false);
      lockListLayoutManager.setVerticalScrollEnabled(true);
      final FragmentActivity activity = getActivity();
      if (activity != null) {
        Timber.d("Reload options");
        activity.supportInvalidateOptionsMenu();
      }
    }
  };

  @SuppressWarnings("WeakerAccess") LockListPresenter presenter;
  @SuppressWarnings("WeakerAccess") boolean firstRefresh;
  private Unbinder unbinder;
  private MenuItem displaySystemItem;
  private MenuItem displayZeroActivityItem;
  private long loadedPresenterKey;
  private long loadedAdapterKey;

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
            firstRefresh = true;
            return new LockListPresenterLoader(getContext());
          }

          @Override public void onPersistentLoaded(@NonNull LockListPresenter persist) {
            presenter = persist;
          }
        });

    loadedAdapterKey = PersistentCache.get()
        .load(KEY_LOAD_ADAPTER, savedInstanceState, new PersistLoader.Callback<LockListAdapter>() {
          @NonNull @Override public PersistLoader<LockListAdapter> createLoader() {
            return new ListAdapterLoader<LockListAdapter>(getContext()) {
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
    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    return view;
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final boolean forceRefresh = getArguments().getBoolean("FORCE_REFRESH", false);
    if (forceRefresh) {
      Timber.d("Force a list refresh");
      firstRefresh = true;
    }

    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();
  }

  @Override public void onStart() {
    super.onStart();
    recyclerView.setAdapter(fastItemAdapter);
    presenter.bindView(this);

    presenter.setFABStateFromPreference();
    if (firstRefresh) {
      Timber.d("Do initial refresh");
      firstRefresh = false;
      refreshList();
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.unbindView();
  }

  @Override public void onResume() {
    super.onResume();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> fab.show(), 300L);
    setActionBarUpEnabled(false);

    getActivity().supportInvalidateOptionsMenu();
  }

  @Override public void onPause() {
    super.onPause();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> fab.hide(), 300L);
  }

  private void setupSwipeRefresh() {
    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);

    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.addItemDecoration(dividerDecoration);
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
    displayZeroActivityItem = menu.findItem(R.id.menu_show_zero_activity);
    presenter.setSystemVisibilityFromPreference();
    presenter.setZeroActivityFromPreference();
  }

  private void setSystemCheckListener() {
    displaySystemItem.setOnMenuItemClickListener(item -> {
      if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
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

  private void setZeroActivityCheckListener() {
    displayZeroActivityItem.setOnMenuItemClickListener(item -> {
      if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
        Timber.d("List is not refreshing. Allow change of system preference");
        if (item.isChecked()) {
          presenter.setZeroActivityHidden();
        } else {
          presenter.setZeroActivityShown();
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

  @SuppressWarnings("WeakerAccess") void setZeroActivityVisible(boolean visible) {
    displayZeroActivityItem.setOnMenuItemClickListener(null);
    displayZeroActivityItem.setChecked(visible);
    setZeroActivityCheckListener();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    boolean handled;
    switch (item.getItemId()) {
      case R.id.menu_settings:
        handled = true;
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
          Timber.d("List is not refreshing. Do settings click");
          showSettingsScreen();
        }
        break;
      default:
        handled = false;
    }
    return handled || super.onOptionsItemSelected(item);
  }

  private void showSettingsScreen() {
    final FragmentManager fragmentManager = getFragmentManager();
    if (fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null) {
      final FragmentActivity fragmentActivity = getActivity();
      if (fragmentActivity instanceof MainActivity) {
        final MainActivity mainActivity = (MainActivity) fragmentActivity;
        final View containerView = getView();
        final View menuItemView = mainActivity.getSettingsMenuItemView();
        if (containerView != null) {
          fragmentManager.beginTransaction()
              .replace(R.id.main_view_container,
                  SettingsFragment.newInstance(menuItemView, containerView), SettingsFragment.TAG)
              .addToBackStack(null)
              .commit();
        }
      }
    }
  }

  @Override public void setSystemVisible() {
    setSystemVisible(true);
  }

  @Override public void setSystemInvisible() {
    setSystemVisible(false);
  }

  @Override public void setZeroActivityHidden() {
    setZeroActivityVisible(false);
  }

  @Override public void setZeroActivityShown() {
    setZeroActivityVisible(true);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    PersistentCache.get().saveKey(outState, KEY_PRESENTER, loadedPresenterKey);
    PersistentCache.get().saveKey(outState, KEY_LOAD_ADAPTER, loadedAdapterKey);
    super.onSaveInstanceState(outState);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();

    recyclerView.setOnClickListener(null);
    recyclerView.setLayoutManager(null);
    recyclerView.setAdapter(null);

    fab.setOnClickListener(null);
    swipeRefreshLayout.setOnRefreshListener(null);
    taskMap.clear();
    handler.removeCallbacksAndMessages(null);
    unbinder.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    if (!getActivity().isChangingConfigurations()) {
      PersistentCache.get().unload(loadedPresenterKey);
      PersistentCache.get().unload(loadedAdapterKey);
    }
  }

  private void setupFAB() {
    fab.setOnClickListener(view -> presenter.clickPinFAB());
    AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
  }

  @Override public void setFABStateEnabled() {
    final AsyncMap.Entry fabIconTask = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_lock_outline_24dp, new RXLoader())
        .into(fab);
    taskMap.put("fab", fabIconTask);
  }

  @Override public void setFABStateDisabled() {
    final AsyncMap.Entry fabIconTask = AsyncDrawable.with(getContext())
        .load(R.drawable.ic_lock_open_24dp, new RXLoader())
        .into(fab);
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
    if (!swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.setRefreshing(true);
    }

    fastItemAdapter.add(new LockListItem(entry));
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    onListPopulated();
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    // If we use the first item we get a weird location, try a different item
    final View listTargetView = recyclerView.findViewHolderForAdapterPosition(1).itemView;

    final TapTarget fabTarget = TapTarget.forView(fab, getString(R.string.getting_started),
        getString(R.string.getting_started_desc))
        .drawShadow(true)
        .tintTarget(false)
        .cancelable(false);

    final TapTarget listTarget =
        TapTarget.forView(listTargetView, getString(R.string.getting_started),
            getString(R.string.getting_started_desc))
            .drawShadow(true)
            .tintTarget(false)
            .cancelable(false);

    new TapTargetSequence(getActivity()).targets(fabTarget, listTarget)
        .listener(new TapTargetSequence.Listener() {
          @Override public void onSequenceFinish() {
            if (presenter != null) {
              presenter.setOnBoard();
            }
          }

          @Override public void onSequenceCanceled() {

          }
        })
        .start();
  }

  @Override public void onListCleared() {
    Timber.d("Prepare for refresh");
    firstRefresh = true;

    Timber.d("onListCleared");
    handler.post(startRefreshRunnable);
    handler.post(() -> fab.hide());
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    handler.post(stopRefreshRunnable);
    handler.post(() -> fab.show());

    Timber.d("We have refreshed");
    firstRefresh = false;

    presenter.showOnBoarding();
  }

  @Override public void refreshList() {
    final int oldSize = fastItemAdapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      fastItemAdapter.remove(i);
    }

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

  @Override
  public void processDatabaseModifyEvent(boolean isChecked, int position, @NonNull AppEntry entry) {
    Timber.d("Received a database modify event request for %s at %d [%s]", entry.packageName(),
        position, isChecked ? "LOCK" : "NO LOCK");
    presenter.modifyDatabaseEntry(isChecked, position, entry.packageName(), null, entry.system());
  }

  @Override public void displayLockInfoDialog(@NonNull AppEntry entry) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), LockInfoDialog.newInstance(entry),
        "lock_info");
  }
}
