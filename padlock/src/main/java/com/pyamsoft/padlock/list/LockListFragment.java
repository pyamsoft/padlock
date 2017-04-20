/*
 * Copyright 2017 Peter Kenji Yamanaka
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

package com.pyamsoft.padlock.list;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.pyamsoft.padlock.Injector;
import com.pyamsoft.padlock.PadLock;
import com.pyamsoft.padlock.R;
import com.pyamsoft.padlock.databinding.FragmentLockListBinding;
import com.pyamsoft.padlock.model.AppEntry;
import com.pyamsoft.padlock.onboard.list.OnboardListDialog;
import com.pyamsoft.padlock.pin.PinEntryDialog;
import com.pyamsoft.padlock.service.PadLockService;
import com.pyamsoft.pydroid.design.fab.HideScrollFABBehavior;
import com.pyamsoft.pydroid.design.util.FABUtil;
import com.pyamsoft.pydroid.ui.app.fragment.ActionBarFragment;
import com.pyamsoft.pydroid.ui.loader.ImageLoader;
import com.pyamsoft.pydroid.ui.loader.LoaderHelper;
import com.pyamsoft.pydroid.ui.loader.loaded.Loaded;
import com.pyamsoft.pydroid.ui.rating.RatingDialog;
import com.pyamsoft.pydroid.util.AnimUtil;
import com.pyamsoft.pydroid.util.DialogUtil;
import javax.inject.Inject;
import timber.log.Timber;

public class LockListFragment extends ActionBarFragment {

  @NonNull public static final String TAG = "LockListFragment";
  @SuppressWarnings("WeakerAccess") FastItemAdapter<LockListItem> fastItemAdapter;
  @SuppressWarnings("WeakerAccess") @Inject LockListPresenter presenter;
  @SuppressWarnings("WeakerAccess") FragmentLockListBinding binding;
  @NonNull Loaded fabIconTask = LoaderHelper.empty();
  @NonNull final LockListPresenter.FABStateCallback fabStateCallback =
      new LockListPresenter.FABStateCallback() {

        @Override public void onSetFABStateEnabled() {
          fabIconTask = LoaderHelper.unload(fabIconTask);
          fabIconTask =
              ImageLoader.fromResource(R.drawable.ic_lock_outline_24dp).into(binding.applistFab);
        }

        @Override public void onSetFABStateDisabled() {
          fabIconTask = LoaderHelper.unload(fabIconTask);
          fabIconTask =
              ImageLoader.fromResource(R.drawable.ic_lock_open_24dp).into(binding.applistFab);
        }
      };
  boolean listDoneRefreshing;
  @Nullable private MenuItem displaySystemItem;
  @Nullable private DividerItemDecoration dividerDecoration;
  private FilterListDelegate filterListDelegate;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    listDoneRefreshing = false;
    setHasOptionsMenu(true);

    Injector.get().provideComponent().plusLockListComponent().inject(this);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    filterListDelegate = new FilterListDelegate();
    fastItemAdapter = new FastItemAdapter<>();
    binding = FragmentLockListBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    filterListDelegate.onViewCreated(fastItemAdapter);
    setupRecyclerView();
    setupSwipeRefresh();
    setupFAB();
  }

  @Override public void onStart() {
    super.onStart();

    presenter.registerOnBus(new LockListPresenter.BusCallback() {

      @Override public void onMasterPinCreateSuccess() {
        fabStateCallback.onSetFABStateEnabled();
        final View v = getView();
        if (v != null) {
          Snackbar.make(v, "PadLock Enabled", Snackbar.LENGTH_SHORT).show();
        }
      }

      @Override public void onMasterPinCreateFailure() {
        Toast.makeText(getContext(), "Error: Mismatched PIN", Toast.LENGTH_SHORT).show();
      }

      @Override public void onMasterPinClearSuccess() {
        fabStateCallback.onSetFABStateDisabled();
        final View v = getView();
        if (v != null) {
          Snackbar.make(v, "PadLock Disabled", Snackbar.LENGTH_SHORT).show();
        }
      }

      @Override public void onMasterPinClearFailure() {
        Toast.makeText(getContext(), "Error: Invalid PIN", Toast.LENGTH_SHORT).show();
      }
    });

    presenter.setFABStateFromPreference(fabStateCallback);

    if (!listDoneRefreshing) {
      refreshList(false);
    }
  }

  @Override public void onStop() {
    super.onStop();
    presenter.stop();
  }

  @Override public void onResume() {
    super.onResume();
    setActionBarUpEnabled(false);
    AnimUtil.popShow(binding.applistFab, 300, 400);
  }

  @Override public void onPause() {
    super.onPause();
    AnimUtil.popHide(binding.applistFab, 300, 400);
  }

  private void setupSwipeRefresh() {
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.amber700,
        R.color.blue700, R.color.amber500);
    binding.applistSwipeRefresh.setOnRefreshListener(() -> {
      Timber.d("onRefresh");
      refreshList(true);
    });
  }

  private void setupRecyclerView() {
    dividerDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
    fastItemAdapter.withSelectable(true);
    fastItemAdapter.withOnClickListener((view, iAdapter, item, i) -> {
      displayLockInfoFragment(item.getModel());
      return true;
    });

    LinearLayoutManager manager = new LinearLayoutManager(getContext());
    manager.setItemPrefetchEnabled(true);
    manager.setInitialPrefetchItemCount(3);
    binding.applistRecyclerview.setLayoutManager(manager);
    binding.applistRecyclerview.setClipToPadding(false);
    binding.applistRecyclerview.setHasFixedSize(false);
    binding.applistRecyclerview.addItemDecoration(dividerDecoration);
    binding.applistRecyclerview.setAdapter(fastItemAdapter);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.locklist_menu, menu);
    inflater.inflate(R.menu.search_menu, menu);
  }

  @Override public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    setupDisplaySystemVisibleItem(menu);
    filterListDelegate.onPrepareOptionsMenu(menu, fastItemAdapter);
  }

  private void setupDisplaySystemVisibleItem(final @NonNull Menu menu) {
    displaySystemItem = menu.findItem(R.id.menu_is_system);
    presenter.setSystemVisibilityFromPreference(new LockListPresenter.SystemVisibilityCallback() {
      @Override public void onSetSystemVisible() {
        setSystemVisible(true);
      }

      @Override public void onSetSystemInvisible() {
        setSystemVisible(false);
      }
    });
  }

  @SuppressWarnings("WeakerAccess") void setSystemVisible(boolean visible) {
    if (displaySystemItem == null) {
      throw new IllegalStateException("DisplaySystem menu item is NULL.");
    }

    displaySystemItem.setChecked(visible);
  }

  @Override public void onDestroyView() {
    filterListDelegate.onDestroyView();
    displaySystemItem = null;

    binding.applistRecyclerview.removeItemDecoration(dividerDecoration);
    binding.applistRecyclerview.setOnClickListener(null);
    binding.applistRecyclerview.setLayoutManager(null);
    binding.applistRecyclerview.setAdapter(null);

    binding.applistFab.setOnClickListener(null);
    binding.applistSwipeRefresh.setOnRefreshListener(null);

    fabIconTask = LoaderHelper.unload(fabIconTask);
    binding.unbind();
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    PadLock.getRefWatcher(this).watch(this);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_is_system:
        if (binding.applistSwipeRefresh != null && !binding.applistSwipeRefresh.isRefreshing()) {
          Timber.d("List is not refreshing. Allow change of system preference");
          if (item.isChecked()) {
            presenter.setSystemInvisible();
          } else {
            presenter.setSystemVisible();
          }

          refreshList(true);
        }
        break;
      default:
        Timber.w("Item selected: %d, do nothing", item.getItemId());
    }
    return super.onOptionsItemSelected(item);
  }

  private void setupFAB() {
    binding.applistFab.setOnClickListener(v -> {
      if (PadLockService.isRunning()) {
        DialogUtil.guaranteeSingleDialogFragment(getActivity(),
            PinEntryDialog.newInstance(getContext().getPackageName(),
                getActivity().getClass().getName()), PinEntryDialog.TAG);
      } else {
        DialogUtil.guaranteeSingleDialogFragment(getActivity(), new AccessibilityRequestDialog(),
            "accessibility");
      }
    });
    FABUtil.setupFABBehavior(binding.applistFab, new HideScrollFABBehavior(24));
  }

  public void onCompletedOnboarding() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof RatingDialog.ChangeLogProvider) {
      RatingDialog.showRatingDialog(activity, (RatingDialog.ChangeLogProvider) activity, false);
    }
  }

  @SuppressWarnings("WeakerAccess") void displayLockInfoFragment(@NonNull AppEntry entry) {
    DialogUtil.guaranteeSingleDialogFragment(getActivity(), LockInfoDialog.newInstance(entry),
        LockInfoDialog.TAG);
  }

  void refreshList(boolean force) {
    fastItemAdapter.clear();
    presenter.populateList(force, new LockListPresenter.PopulateListCallback() {

      private void setRefreshing(boolean refresh) {
        binding.applistSwipeRefresh.post(() -> {
          if (binding != null) {
            if (binding.applistSwipeRefresh != null) {
              binding.applistSwipeRefresh.setRefreshing(refresh);
            }
          }
        });

        final FragmentActivity activity = getActivity();
        if (activity != null) {
          Timber.d("Reload options");
          activity.supportInvalidateOptionsMenu();
        }
      }

      @Override public void onListPopulateBegin() {
        listDoneRefreshing = false;
        setRefreshing(true);
        fastItemAdapter.clear();
        binding.applistFab.hide();
      }

      @Override public void onEntryAddedToList(@NonNull AppEntry entry) {
        fastItemAdapter.add(new LockListItem(entry));
      }

      @Override public void onListPopulated() {
        listDoneRefreshing = true;
        setRefreshing(false);
        binding.applistFab.show();

        if (fastItemAdapter.getAdapterItemCount() > 1) {
          Timber.d("We have refreshed");
          presenter.showOnBoarding(new LockListPresenter.OnboardingCallback() {
            @Override public void onShowOnboarding() {
              Timber.d("Show onboarding");
              DialogUtil.onlyLoadOnceDialogFragment(getActivity(), new OnboardListDialog(),
                  OnboardListDialog.TAG);
            }

            @Override public void onOnboardingComplete() {
              onCompletedOnboarding();
            }
          });
        } else {
          Toast.makeText(getContext(), "Error while loading list. Please try again.",
              Toast.LENGTH_SHORT).show();
        }
      }

      @Override public void onListPopulateError() {
        DialogUtil.guaranteeSingleDialogFragment(getActivity(), new ErrorDialog(), "error");
      }
    });
  }
}
