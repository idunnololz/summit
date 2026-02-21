package com.idunnololz.summit.uploads

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.dispose
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.dto.lemmy.LocalImage
import com.idunnololz.summit.databinding.FragmentUploadsBinding
import com.idunnololz.summit.databinding.UploadItemErrorItemBinding
import com.idunnololz.summit.databinding.UploadItemLoadingItemBinding
import com.idunnololz.summit.databinding.UploadItemMediaItemBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToFullDateTime
import com.idunnololz.summit.util.getImageErrorDrawable
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import com.idunnololz.summit.util.showProgressBarIfNeeded
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadsFragment : BaseFragment<FragmentUploadsBinding>() {

  private val viewModel: UploadsViewModel by viewModels()

  @Inject
  lateinit var offlineManager: OfflineManager

  private val confirmDeleteMediaLauncher = newAlertDialogLauncher("confirm_delete") {
    if (it.isOk) {
      it.extras?.getString("filename")?.let { filename ->
        it.extras?.getString("delete_token")?.let { deleteToken ->
          viewModel.deleteImage(deleteToken, filename)
        }
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentUploadsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

        navBarController.updatePaddingForNavBar(binding.contentContainer)
      }

      setupToolbar(toolbar, getString(R.string.uploads))

      viewModel.fetchUploads()

      val adapter = UploadsAdapter(
        context = context,
        offlineManager = offlineManager,
        onImageClick = { sharedElementView, url ->
          getMainActivity()?.openImage(
            sharedElement = sharedElementView,
            appBar = appBar,
            title = null,
            url = url,
            mimeType = null,
            downloadContext = null,
          )
        },
        onDeleteClick = { image ->
          confirmDeleteMediaLauncher.launchDialog {
            titleResId = R.string.delete_item
            messageResId = R.string.delete_item_desc

            extras.putString("filename", image.pictrs_alias)
            extras.putString("delete_token", image.pictrs_delete_token)

            negativeButtonResId = android.R.string.cancel
            positionButtonResId = R.string.delete
          }
        },
      )
      val layoutManager = LinearLayoutManager(context)

      fun fetchPageIfLoadItem(position: Int) {
        (adapter.items.getOrNull(position) as? UploadsEngine.UploadItem.MoreItem)
          ?.let {
            viewModel.loadMoreIfNeeded(it.pageIndex)
          }
      }

      fun checkIfFetchNeeded() {
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val lastPos = layoutManager.findLastVisibleItemPosition()

        for (i in (firstPos - 1)..(lastPos + 1)) {
          fetchPageIfLoadItem(i)
        }
      }

      recyclerView.layoutManager = layoutManager
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter
      recyclerView.addOnScrollListener(
        object : OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            checkIfFetchNeeded()
          }
        },
      )

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.fetchUploads(force = true)
      }

      loadingView.showProgressBar()

      viewLifecycleOwner.lifecycleScope.launch {
        viewModel.itemsFlow.collect {
          swipeRefreshLayout.isRefreshing = false
          loadingView.hideAll()

          adapter.setData(it)
        }
      }

      viewModel.deleteResult.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            viewModel.deleteResult.setIdle()

            ErrorDialogFragment.show(
              message = getString(R.string.error_delete_item_failed),
              error = it.error,
              fm = childFragmentManager,
            )
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBarIfNeeded(swipeRefreshLayout)
          }
          is StatefulData.NotStarted -> {
            loadingView.hideAll()
          }
          is StatefulData.Success -> {
            loadingView.hideAll()
          }
        }
      }
    }
  }

  private class UploadsAdapter(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val onImageClick: (sharedElementView: View, url: String) -> Unit,
    private val onDeleteClick: (image: LocalImage) -> Unit,
  ) : RecyclerView.Adapter<ViewHolder>() {

    val items
      get() = adapterHelper.items

    private var data: List<UploadsEngine.UploadItem>? = null

    private val adapterHelper = AdapterHelper<UploadsEngine.UploadItem>(
      { old, new ->
        old::class == new::class &&
          when (old) {
            is UploadsEngine.UploadItem.ErrorItem -> {
              old.pageIndex == (new as UploadsEngine.UploadItem.ErrorItem).pageIndex
            }
            is UploadsEngine.UploadItem.LoadingItem -> {
              old.pageIndex == (new as UploadsEngine.UploadItem.LoadingItem).pageIndex
            }
            is UploadsEngine.UploadItem.MoreItem -> {
              old.pageIndex == (new as UploadsEngine.UploadItem.MoreItem).pageIndex
            }
            is UploadsEngine.UploadItem.MediaItem -> {
              old.media.local_image.pictrs_alias ==
                (new as UploadsEngine.UploadItem.MediaItem).media.local_image.pictrs_alias
            }
          }
      },
    ).apply {
      addItemType(
        clazz = UploadsEngine.UploadItem.MediaItem::class,
        inflateFn = UploadItemMediaItemBinding::inflate,
      ) { item, b, h ->
        val instance = item.instance
        val pictrsAlias = item.media.local_image.pictrs_alias
        val url = "https://$instance/pictrs/image/$pictrsAlias"

        b.title.text = dateStringToFullDateTime(item.media.local_image.published)

        b.imageView.transitionName = "image_${h.absoluteAdapterPosition}"
        b.imageView.dispose()
        b.imageView.load(newShimmerDrawableSquare(context))

        offlineManager.fetchImageWithError(
          imageView = b.imageView,
          url = url,
          listener = {
            b.imageView.load(it)
          },
          errorListener = {
            b.imageView.dispose()
            b.imageView.setImageDrawable(context.getImageErrorDrawable())
          },
        )

        b.imageView.setOnClickListener {
          onImageClick(b.imageView, url)
        }
        b.delete.setOnClickListener {
          onDeleteClick(item.media.local_image)
        }
      }
      addItemType(
        clazz = UploadsEngine.UploadItem.LoadingItem::class,
        inflateFn = UploadItemLoadingItemBinding::inflate,
      ) { item, b, h ->
        b.loadingView.showProgressBar()
      }
      addItemType(
        clazz = UploadsEngine.UploadItem.MoreItem::class,
        inflateFn = UploadItemLoadingItemBinding::inflate,
      ) { item, b, h ->
        b.loadingView.showProgressBar()
      }
      addItemType(
        clazz = UploadsEngine.UploadItem.ErrorItem::class,
        inflateFn = UploadItemErrorItemBinding::inflate,
      ) { item, b, h ->
        b.loadingView.showDefaultErrorMessageFor(item.error)
      }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int = adapterHelper.itemCount

    fun setData(data: List<UploadsEngine.UploadItem>) {
      this.data = data

      refreshItems()
    }

    private fun refreshItems() {
      val data = data ?: return

      adapterHelper.setItems(data, this)
    }
  }
}
