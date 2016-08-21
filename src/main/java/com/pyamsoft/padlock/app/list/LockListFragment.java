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
import android.support.v4.content.ContextCompat;
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
import com.pyamsoft.padlock.Singleton;
import com.pyamsoft.padlock.app.base.ErrorDialog;
import com.pyamsoft.padlock.app.lock.MasterPinSubmitCallback;
import com.pyamsoft.padlock.app.lock.PinEntryDialog;
import com.pyamsoft.padlock.app.main.MainActivity;
import com.pyamsoft.padlock.app.settings.SettingsFragment;
import com.pyamsoft.padlock.dagger.db.DBPresenter;
import com.pyamsoft.padlock.dagger.list.AdapterPresenter;
import com.pyamsoft.padlock.dagger.list.LockListPresenter;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.pydroid.base.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.base.fragment.CircularRevealFragmentUtil;
import com.pyamsoft.pydroid.base.presenter.Presenter;
import com.pyamsoft.pydroid.behavior.HideScrollFABBehavior;
import com.pyamsoft.pydroid.tool.AsyncDrawable;
import com.pyamsoft.pydroid.tool.AsyncDrawableMap;
import com.pyamsoft.pydroid.tool.DataHolderFragment;
import com.pyamsoft.pydroid.tool.DividerItemDecoration;
import com.pyamsoft.pydroid.util.AppUtil;
import javax.inject.Inject;
import rx.Subscription;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

public final class LockListFragment extends ActionBarFragment
    implements LockListPresenter.LockList, PinEntryDialogRequest, MasterPinSubmitCallback {

  @NonNull public static final String TAG = "LockListFragment";
  @NonNull private static final String PIN_DIALOG_TAG = "pin_dialog";
  private static final int KEY_PRESENTER = 0;
  private static final int KEY_ADAPTER_PRESENTER = 1;
  private static final int KEY_DB_PRESENTER = 2;
  @NonNull private final Handler handler = new Handler(Looper.getMainLooper());
  @NonNull private final AsyncDrawableMap taskMap = new AsyncDrawableMap();
  @BindView(R.id.applist_fab) FloatingActionButton fab;
  @BindView(R.id.applist_recyclerview) RecyclerView recyclerView;
  @BindView(R.id.applist_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
  @Inject LockListPresenter presenter;
  @Inject AdapterPresenter<AppEntry, LockListAdapter.ViewHolder> adapterPresenter;
  @Inject DBPresenter dbPresenter;
  private DataHolderFragment<Presenter> presenterDataHolder;
  private LockListAdapter adapter;
  private LockListLayoutManager lockListLayoutManager;
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
  private Unbinder unbinder;
  private MenuItem displaySystemItem;
  private boolean firstRefresh;

  public static LockListFragment newInstance(int cX, int cY) {
    final Bundle args = CircularRevealFragmentUtil.bundleArguments(cX, cY, 0);
    final LockListFragment fragment = new LockListFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);

    presenterDataHolder = DataHolderFragment.getInstance(getActivity(), "lock_list_presenters");
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    final LockListPresenter lockListPresenter =
        (LockListPresenter) presenterDataHolder.pop(KEY_PRESENTER);
    @SuppressWarnings("unchecked") final AdapterPresenter<AppEntry, LockListAdapter.ViewHolder>
        entryAdapterPresenter =
        (AdapterPresenter<AppEntry, LockListAdapter.ViewHolder>) presenterDataHolder.pop(
            KEY_ADAPTER_PRESENTER);
    final DBPresenter lockDBPresenter = (DBPresenter) presenterDataHolder.pop(KEY_DB_PRESENTER);
    if (lockListPresenter == null || entryAdapterPresenter == null || lockDBPresenter == null) {
      Timber.d("Create new presenters");
      firstRefresh = true;
      Singleton.Dagger.with(getContext()).plusLockList().inject(this);
    } else {
      Timber.d("Load cached presenters");
      firstRefresh = false;
      presenter = lockListPresenter;
      adapterPresenter = entryAdapterPresenter;
      dbPresenter = lockDBPresenter;
    }

    adapter = new LockListAdapter(this, adapterPresenter, dbPresenter);

    final View view = inflater.inflate(R.layout.fragment_applist, container, false);
    unbinder = ButterKnife.bind(this, view);
    presenter.bindView(this);
    adapter.onCreate();
    return view;
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    CircularRevealFragmentUtil.runCircularRevealOnViewCreated(view, getArguments());
    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();

    if (firstRefresh) {
      Timber.d("Do initial refresh");
      firstRefresh = false;
      refreshList();
    }
  }

  private void setupSwipeRefresh() {
    swipeRefreshLayout.setColorSchemeResources(R.color.blue500, R.color.amber700, R.color.blue700,
        R.color.amber500);
    swipeRefreshLayout.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList();
    });
  }

  @Override public void onResume() {
    super.onResume();
    handler.postDelayed(() -> fab.show(new FloatingActionButton.OnVisibilityChangedListener() {
      @Override public void onShown(FloatingActionButton fab) {
        super.onShown(fab);
        presenter.showOnBoarding();
      }
    }), 300L);
    presenter.resume();
    setActionBarUpEnabled(false);
  }

  @Override public void onPause() {
    super.onPause();
    handler.postDelayed(() -> fab.hide(), 300L);
    presenter.pause();
  }

  private void setupRecyclerView() {
    lockListLayoutManager = new LockListLayoutManager(getContext());
    lockListLayoutManager.setVerticalScrollEnabled(true);
    final RecyclerView.ItemDecoration dividerDecoration =
        new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST);

    recyclerView.setLayoutManager(lockListLayoutManager);
    recyclerView.setAdapter(adapter);
    recyclerView.addItemDecoration(dividerDecoration);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    Timber.d("onCreateOptionsMenu");
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupLockListMenuItems(menu);
  }

  private void setupLockListMenuItems(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    presenter.setSystemVisibilityFromPreference();
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

  private void setSystemVisible(boolean visible) {
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

  private void showSettingsScreen() {
    final FragmentActivity fragmentActivity = getActivity();
    if (fragmentActivity instanceof MainActivity) {
      final MainActivity mainActivity = (MainActivity) fragmentActivity;
      final FragmentManager fragmentManager = getFragmentManager();
      final View containerView = getView();
      final View menuItemView = mainActivity.getSettingsMenuItemView();
      if (fragmentManager.findFragmentByTag(SettingsFragment.TAG) == null
          && containerView != null) {
        fragmentManager.beginTransaction()
            .replace(R.id.main_view_container,
                SettingsFragment.newInstance(menuItemView, containerView), SettingsFragment.TAG)
            .addToBackStack(null)
            .commit();
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
    adapter.onDestroy();
    presenter.unbindView(!getActivity().isChangingConfigurations());

    taskMap.clear();
    handler.removeCallbacksAndMessages(null);
    unbinder.unbind();
  }

  private void setupFAB() {
    fab.setOnClickListener(view -> presenter.clickPinFAB());
    AppUtil.setupFABBehavior(fab, new HideScrollFABBehavior(24));
    presenter.setFABStateFromPreference();
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

    adapter.addItem(entry);
  }

  @Override public void onStart() {
    super.onStart();
    adapter.onStart();
  }

  @Override public void onStop() {
    super.onStop();
    adapter.onStop();
  }

  @Override public void onListPopulateError() {
    AppUtil.guaranteeSingleDialogFragment(getFragmentManager(), new ErrorDialog(), "error");
  }

  @Override public void showOnBoarding() {
    Timber.d("Show onboarding");
    // KLUDGE dismissed when orientation is changed
    new MaterialShowcaseView.Builder(getActivity()).setTarget(fab)
        .setTargetTouchable(false)
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
            presenter.setOnBoard();
          }
        })
        .build()
        .show(getActivity());
  }

  @Override public void onListCleared() {
    Timber.d("onListCleared");
    handler.post(startRefreshRunnable);
    handler.post(() -> fab.hide());
  }

  @Override public void onListPopulated() {
    Timber.d("onListPopulated");
    handler.post(stopRefreshRunnable);
    handler.post(() -> fab.show(new FloatingActionButton.OnVisibilityChangedListener() {
      @Override public void onShown(FloatingActionButton fab) {
        super.onShown(fab);
        presenter.showOnBoarding();
      }
    }));
  }

  @Override public void refreshList() {
    final int oldSize = adapter.getItemCount() - 1;
    for (int i = oldSize; i >= 0; --i) {
      adapter.removeItem();
    }
    onListCleared();
    presenter.populateList();
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    if (getActivity().isChangingConfigurations()) {
      presenterDataHolder.put(KEY_PRESENTER, presenter);
      presenterDataHolder.put(KEY_ADAPTER_PRESENTER, adapterPresenter);
      presenterDataHolder.put(KEY_DB_PRESENTER, dbPresenter);
    } else {
      presenterDataHolder.clear();
    }
    super.onSaveInstanceState(outState);
  }
}
