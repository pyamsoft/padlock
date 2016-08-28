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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
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
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.app.db.DBPresenter;
import com.pyamsoft.padlock.app.db.DBProgressDialog;
import com.pyamsoft.padlock.app.lock.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.base.activity.adapter.ListAdapterLoader;
import com.pyamsoft.pydroid.base.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.base.fragment.CircularRevealFragmentUtil;
import com.pyamsoft.pydroid.behavior.HideScrollFABBehavior;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.tool.DividerItemDecoration;
import com.pyamsoft.pydroid.util.AppUtil;
import rx.Subscription;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public class LockListFragment extends ActionBarFragment
    implements LockListPresenter.LockList, PinEntryDialogRequest, MasterPinSubmitCallback,
    DBPresenter.DBView {

  @NonNull public static final String TAG = "LockListFragment";
  @NonNull static final String PIN_DIALOG_TAG = "pin_dialog";
  static final int KEY_PRESENTER = 0;
  static final int KEY_ADAPTER_PRESENTER = 1;
  static final int KEY_DB_PRESENTER = 2;
  @NonNull final Handler handler = new Handler(Looper.getMainLooper());
  @NonNull final AsyncDrawableMap taskMap = new AsyncDrawableMap();
  @BindView(R.id.applist_fab) FloatingActionButton fab;
  @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  LockListAdapter fastItemAdapter;
  LockListLayoutManager lockListLayoutManager;
  @NonNull final Runnable startRefreshRunnable = new Runnable() {
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
  @NonNull final Runnable stopRefreshRunnable = new Runnable() {
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

  DBPresenter dbPresenter;
  LockListPresenter presenter;
  boolean firstRefresh;
  Unbinder unbinder;
  MenuItem displaySystemItem;

  public static LockListFragment newInstance(int cX, int cY) {
    final Bundle args = CircularRevealFragmentUtil.bundleArguments(cX, cY, 600L);
    final LockListFragment fragment = new LockListFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    getLoaderManager().initLoader(KEY_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<LockListPresenter>() {
          @Override public Loader<LockListPresenter> onCreateLoader(int id, Bundle args) {
            firstRefresh = true;
            return new LockListPresenterLoader(getContext());
          }

          @Override
          public void onLoadFinished(Loader<LockListPresenter> loader, LockListPresenter data) {
            presenter = data;
          }

          @Override public void onLoaderReset(Loader<LockListPresenter> loader) {
            presenter = null;
          }
        });

    getLoaderManager().initLoader(KEY_ADAPTER_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<LockListAdapter>() {
          @Override public Loader<LockListAdapter> onCreateLoader(int id, Bundle args) {
            return new ListAdapterLoader<LockListAdapter>(getContext()) {
              @NonNull @Override protected LockListAdapter loadAdapter() {
                return new LockListAdapter();
              }
            };
          }

          @Override
          public void onLoadFinished(Loader<LockListAdapter> loader, LockListAdapter data) {
            fastItemAdapter = data;
          }

          @Override public void onLoaderReset(Loader<LockListAdapter> loader) {
            fastItemAdapter = null;
          }
        });

    getLoaderManager().initLoader(KEY_DB_PRESENTER, null,
        new LoaderManager.LoaderCallbacks<DBPresenter>() {
          @Override public Loader<DBPresenter> onCreateLoader(int id, Bundle args) {
            firstRefresh = true;
            return new DBPresenterLoader(getContext());
          }

          @Override public void onLoadFinished(Loader<DBPresenter> loader, DBPresenter data) {
            dbPresenter = data;
          }

          @Override public void onLoaderReset(Loader<DBPresenter> loader) {
            dbPresenter = null;
          }
        });

    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    return view;
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (firstRefresh) {
      CircularRevealFragmentUtil.runCircularRevealOnViewCreated(view, getArguments());
    }

    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();
  }

  @Override public void onResume() {
    super.onResume();

    recyclerView.setAdapter(fastItemAdapter);
    presenter.bindView(this);
    dbPresenter.bindView(this);

    presenter.setFABStateFromPreference();
    presenter.showOnBoarding();
    if (firstRefresh) {
      Timber.d("Do initial refresh");
      firstRefresh = false;
      refreshList();
    }

    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> fab.show(), 300L);
    setActionBarUpEnabled(false);

    getActivity().supportInvalidateOptionsMenu();
  }

  @Override public void onPause() {
    super.onPause();
    handler.removeCallbacksAndMessages(null);
    handler.postDelayed(() -> fab.hide(), 300L);

    presenter.unbindView();
    dbPresenter.unbindView();
  }

  void setupSwipeRefresh() {
    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);

    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    Timber.d("onCreateOptionsMenu");
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (isResumed()) {
      setupLockListMenuItems(menu);
    }
  }

  void setupLockListMenuItems(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    presenter.setSystemVisibilityFromPreference();
  }

  void setSystemCheckListener() {
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

  void setSystemVisible(boolean visible) {
    displaySystemItem.setOnMenuItemClickListener(null);
    displaySystemItem.setChecked(visible);
    setSystemCheckListener();
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

  void showSettingsScreen() {
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

  void setupFAB() {
    fab.setOnClickListener(view -> presenter.clickPinFAB());
    AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
  }

  @Override public void setFABStateEnabled() {
    final Subscription fabIconTask =
        AsyncDrawable.with(getContext()).load(R.drawable.ic_lock_outline_24dp).into(fab);
    taskMap.put("fab", fabIconTask);
  }

  @Override public void setFABStateDisabled() {
    final Subscription fabIconTask =
        AsyncDrawable.with(getContext()).load(R.drawable.ic_lock_open_24dp).into(fab);
    taskMap.put("fab", fabIconTask);
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

  @Override public void onPinFABClicked() {
    onPinEntryDialogRequested(getContext().getPackageName(), getActivity().getClass().getName());
  }

  @Override public void onEntryAddedToList(@NonNull AppEntry entry) {
    Timber.d("Add entry: %s", entry);

    // In case the configuration changes, we do the animation again
    if (!swipeRefreshLayout.isRefreshing()) {
      swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
    }

    fastItemAdapter.add(new LockListItem(entry));
  }

  @Override public void onListPopulateError() {
    Timber.e("onListPopulateError");
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    // KLUDGE dismissed when orientation is changed
    new MaterialShowcaseView.Builder(getActivity()).setTargetTouchable(false)
        .setTarget(fab)
        .setMaskColour(ContextCompat.getColor(getContext(), R.color.blue500))
        .setTitleText(R.string.app_name)
        .setContentText(R.string.getting_started)
        .setDismissText(R.string.got_it)
        .setListener(new IShowcaseListener() {
          @Override public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {
            Timber.d("onShowcaseDisplayed");
          }

          @Override public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
            Timber.d("onShowcaseDismissed");
            if (presenter != null) {
              presenter.setOnBoard();
            }
          }
        })
        .build()
        .show(getActivity());
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
  }

  @Override public void refreshList() {
    final int oldSize = fastItemAdapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      fastItemAdapter.remove(i);
    }

    onListCleared();
    presenter.populateList();
  }

  @Override public void onDBCreateEvent(int position) {
    handler.postDelayed(() -> DBProgressDialog.remove(getFragmentManager()), 500L);
    fastItemAdapter.onDBCreateEvent(position);
  }

  @Override public void onDBDeleteEvent(int position) {
    handler.postDelayed(() -> DBProgressDialog.remove(getFragmentManager()), 500L);
    fastItemAdapter.onDBDeleteEvent(position);
  }

  @Override public void onDBError() {
    handler.postDelayed(() -> DBProgressDialog.remove(getFragmentManager()), 500L);
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override
  public void displayDBProgressDialog(int position, boolean checked, @NonNull AppEntry entry) {
    DBProgressDialog.add(getFragmentManager(), entry.name());
    dbPresenter.attemptDBModification(position, checked, entry.packageName(), null, entry.system());
  }

  @Override public void displayLockInfoDialog(@NonNull AppEntry entry) {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), LockInfoDialog.newInstance(entry),
        "lock_info");
  }
}
