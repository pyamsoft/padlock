package com.pyamsoft.padlock.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.padlock.R
import com.pyamsoft.padlock.databinding.FragmentLockListBinding
import com.pyamsoft.padlock.helper.ListStateUtil
import com.pyamsoft.padlock.helper.tintIcon
import com.pyamsoft.padlock.model.list.AppEntry
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.app.activity.ToolbarActivity
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.popHide
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.ui.widget.HideOnScrollListener
import com.pyamsoft.pydroid.ui.widget.RefreshLatch
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

internal class LockListViewImpl @Inject internal constructor(
  private val toolbarActivity: ToolbarActivity,
  private val activity: FragmentActivity,
  private val owner: LifecycleOwner,
  private val inflater: LayoutInflater,
  private val container: ViewGroup?,
  private val savedInstanceState: Bundle?,
  private val theming: Theming,
  private val listStateTag: String,
  private val imageLoader: ImageLoader
) : LockListView, LifecycleObserver {

  private lateinit var binding: FragmentLockListBinding
  private lateinit var filterListDelegate: FilterListDelegate
  private lateinit var modelAdapter: ModelAdapter<AppEntry, LockListItem>
  private lateinit var hideScrollListener: RecyclerView.OnScrollListener
  private lateinit var refreshLatch: RefreshLatch

  private var lastPosition: Int = 0
  private var displaySystemItem: MenuItem? = null

  init {
    owner.lifecycle.addObserver(this)
  }

  @OnLifecycleEvent(ON_DESTROY)
  internal fun destroy() {
    owner.lifecycle.removeObserver(this)

    filterListDelegate.onDestroyView()
    binding.apply {
      applistRecyclerview.setOnDebouncedClickListener(null)
      applistRecyclerview.removeOnScrollListener(hideScrollListener)
      applistRecyclerview.layoutManager = null
      applistRecyclerview.adapter = null
      applistFab.setOnDebouncedClickListener(null)
      applistSwipeRefresh.setOnRefreshListener(null)
      unbind()
    }

    modelAdapter.clear()
  }

  override fun getListData(): List<AppEntry> {
    return Collections.unmodifiableList(modelAdapter.models)
  }

  override fun commitListState(outState: Bundle?) {
    if (outState == null) {
      // If we are not commiting to bundle, commit to local var
      lastPosition = ListStateUtil.getCurrentPosition(binding.applistRecyclerview)
    }

    ListStateUtil.saveState(listStateTag, outState, binding.applistRecyclerview)
  }

  override fun onListPopulateBegin() {
    refreshLatch.isRefreshing = true
  }

  override fun onListPopulated() {
    refreshLatch.isRefreshing = false
  }

  override fun onListLoaded(list: List<AppEntry>) {
    modelAdapter.set(list)
  }

  override fun onListPopulateError(onAction: () -> Unit) {
    Snackbreak.long(root(), "Failed to load application list")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onModifyEntryError(onAction: () -> Unit) {
    Snackbreak.long(root(), "Failed to modify application list")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onDatabaseChangeError(onAction: () -> Unit) {
    Snackbreak.long(root(), "Failed realtime monitoring for application list")
        .setAction("Retry") { onAction() }
        .show()
  }

  override fun onDatabaseChangeReceived(
    index: Int,
    entry: AppEntry
  ) {
    modelAdapter.set(index, entry)
  }

  override fun create() {
    binding = FragmentLockListBinding.inflate(inflater, container, false)

    setupRecyclerView()
    setupSwipeRefresh()
    setupFAB()
    setupToolbarMenu()
    prepareFilterDelegate()

    lastPosition = ListStateUtil.restoreState(listStateTag, savedInstanceState)
  }

  override fun onRefreshed(onRefreshed: () -> Unit) {
    refreshLatch = RefreshLatch.create(owner) { loading: Boolean ->
      filterListDelegate.setEnabled(!loading)
      binding.apply {
        applistSwipeRefresh.refreshing(loading)

        if (loading) {
          binding.applistFab.popHide()
        } else {
          binding.applistFab.popShow()
        }
      }

      // Load is done
      if (!loading) {
        if (modelAdapter.adapterItemCount > 0) {
          showRecycler()
          lastPosition = ListStateUtil.restorePosition(lastPosition, binding.applistRecyclerview)
          onRefreshed()
        } else {
          hideRecycler()
        }
      }
    }
  }

  private fun setupFAB() {
    // Attach the FAB to callbacks on the recycler scroll
    hideScrollListener = HideOnScrollListener.withView(binding.applistFab) {
      if (!binding.applistSwipeRefresh.isRefreshing) {
        if (it) {
          binding.applistFab.popShow()
        } else {
          binding.applistFab.popHide()
        }
      }
    }

    binding.applistRecyclerview.addOnScrollListener(hideScrollListener)
  }

  override fun onFabClicked(onClick: () -> Unit) {
    binding.applistFab.setOnDebouncedClickListener { onClick() }
  }

  private fun setupSwipeRefresh() {
    binding.applistSwipeRefresh.setColorSchemeResources(R.color.blue500, R.color.blue700)
  }

  override fun onSwipeRefresh(onSwipe: () -> Unit) {
    binding.applistSwipeRefresh.setOnRefreshListener {
      refreshLatch.forceRefresh()
      onSwipe()
    }
  }

  private fun setupToolbarMenu() {
    toolbarActivity.withToolbar {
      it.inflateMenu(R.menu.locklist_menu)
      it.inflateMenu(R.menu.search_menu)

      it.menu.apply {
        displaySystemItem = findItem(R.id.menu_is_system)
        tintIcon(activity, theming, R.id.menu_search)
      }

    }
  }

  override fun onToolbarMenuItemClicked(onClick: (item: MenuItem) -> Unit) {
    toolbarActivity.withToolbar { toolbar ->
      toolbar.setOnMenuItemClickListener {
        if (!binding.applistSwipeRefresh.isRefreshing) {
          onClick(it)
        }

        return@setOnMenuItemClickListener true
      }
    }
  }

  private fun setupRecyclerView() {
    modelAdapter = ModelAdapter { LockListItem(activity, it) }

    binding.applistRecyclerview.apply {
      layoutManager = LinearLayoutManager(context).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }

      setHasFixedSize(true)
      addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
      adapter = FastAdapter.with<LockListItem, ModelAdapter<AppEntry, LockListItem>>(modelAdapter)

      modelAdapter.fastAdapter.withSelectable(true)
    }

    // First load should show the spinner
    showRecycler()
  }

  override fun onListItemClicked(onClick: (model: AppEntry) -> Unit) {
    modelAdapter.fastAdapter.withOnClickListener { _, _, item, _ ->
      onClick(item.model)
      return@withOnClickListener true
    }
  }

  override fun onSystemVisibilityChanged(visible: Boolean) {
    displaySystemItem?.isChecked = visible
  }

  override fun onFabIconLocked() {
    Timber.d("FAB locked")
    imageLoader.load(R.drawable.ic_lock_outline_24dp)
        .into(binding.applistFab)
        .bind(owner)
  }

  override fun onFabIconPaused() {
    Timber.d("FAB paused")
    imageLoader.load(R.drawable.ic_pause_24dp)
        .into(binding.applistFab)
        .bind(owner)
  }

  override fun onFabIconPermissionDenied() {
    Timber.d("FAB permission")
    imageLoader.load(R.drawable.ic_warning_24dp)
        .mutate { it.tintWith(root().context, R.color.white) }
        .into(binding.applistFab)
        .bind(owner)
  }

  override fun onFabIconUnlocked() {
    Timber.d("FAB unlocked")
    imageLoader.load(R.drawable.ic_lock_open_24dp)
        .into(binding.applistFab)
        .bind(owner)
  }

  private fun showRecycler() {
    binding.apply {
      applistEmpty.visibility = View.GONE
      applistRecyclerview.visibility = View.VISIBLE
    }
  }

  private fun hideRecycler() {
    binding.apply {
      applistRecyclerview.visibility = View.GONE
      applistEmpty.visibility = View.VISIBLE
    }
  }

  private fun prepareFilterDelegate() {
    filterListDelegate = FilterListDelegate()
    filterListDelegate.onViewCreated(modelAdapter)
    toolbarActivity.requireToolbar {
      filterListDelegate.onPrepareOptionsMenu(it.menu, modelAdapter)
    }
  }

  override fun onMasterPinClearSuccess() {
    Snackbreak.short(root(), "PadLock Disabled")
        .show()
  }

  override fun onMasterPinClearFailure() {
    Snackbreak.short(root(), "Failed to clear master pin")
        .show()
  }

  override fun onMasterPinCreateFailure() {
    Snackbreak.short(root(), "Failed to create master pin")
        .show()
  }

  override fun onMasterPinCreateSuccess() {
    Snackbreak.short(root(), "PadLock Enabled")
        .show()
  }

  override fun root(): View {
    return binding.root
  }

}
