package com.idunnololz.summit.video

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentVideoInfoBinding
import com.idunnololz.summit.databinding.ImageInfoInfoItemBinding
import com.idunnololz.summit.image.ImageInfoModel
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class VideoInfoDialogFragment :
  BaseBottomSheetDialogFragment<DialogFragmentVideoInfoBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun show(fragmentManager: FragmentManager, url: String) {
      VideoInfoDialogFragment().apply {
        arguments = VideoInfoDialogFragmentArgs(url).toBundle()
      }.showAllowingStateLoss(fragmentManager, "VideoInfoDialogFragment")
    }
  }

  private val args by navArgs<VideoInfoDialogFragmentArgs>()

  private val viewModel: VideoInfoViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var exoPlayerManagerManager: ExoPlayerManagerManager

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentVideoInfoBinding.inflate(inflater, container, false))

    return binding.root
  }

  @OptIn(UnstableApi::class)
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {

      val adapter = VideoInfoAdapter(context)

      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(false)
      recyclerView.adapter = adapter
      recyclerView.setup(animationsHelper)
      loadingView.setOnRefreshClickListener {
        viewModel.loadVideoInfo(args.url, force = true)
      }

      viewModel.model.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            loadingView.hideAll()

            adapter.setData(it.data)
          }
        }
      }

      viewModel.loadVideoInfo(args.url)
    }
  }

  private class VideoInfoAdapter(
    private val context: Context,
  ) : Adapter<ViewHolder>() {

    sealed interface Item {
      data class InfoItem(
        val title: String,
        val value: String,
      ): Item
    }

    private var data: VideoInfoViewModel.VideoInfoModel? = null

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.InfoItem ->
            old.title == (new as Item.InfoItem).title
        }
      },
    ).apply {
      addItemType(
        Item.InfoItem::class,
        ImageInfoInfoItemBinding::inflate,
      ) { item, b, h ->
        b.title.text = item.title
        b.value.text = item.value

        fun showPopupMenu() {
          PopupMenu(
            context,
            b.root,
            Gravity.NO_GRAVITY,
            0,
            R.style.Theme_App_Widget_Material3_PopupMenu_Overflow,
          ).apply {
            inflate(R.menu.image_info)

            setOnMenuItemClickListener {
              when (it.itemId) {
                R.id.copy_value -> {
                  Utils.copyToClipboard(context, item.value)
                }
              }

              true
            }

            show()
          }
        }

        b.root.setOnClickListener {
          showPopupMenu()
        }
        b.root.setOnLongClickListener {
          showPopupMenu()
          true
        }
      }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setData(data: VideoInfoViewModel.VideoInfoModel) {
      this.data = data

      refreshItems()
    }

    private fun refreshItems() {
      val data = data ?: return
      val newItems = mutableListOf<Item>()

      fun addInfo(title: String, value: String) {
        newItems.add(
          Item.InfoItem(
            title = title,
            value = value,
          )
        )
      }

      addInfo(
        title = context.getString(R.string.url),
        value = data.url,
      )

      for (track in data.tracks) {
        addInfo(
          title = "track",
          value = track.name
        )

        for (format in track.formats) {
          addInfo(
            title = "format.id",
            value = format.id ?: ""
          )
          addInfo(
            title = "format.label",
            value = format.label ?: ""
          )
          addInfo(
            title = "format.language",
            value = format.language ?: ""
          )
          addInfo(
            title = "format.selectionFlags",
            value = format.selectionFlags.toString() ?: ""
          )
          addInfo(
            title = "format.roleFlags",
            value = format.roleFlags.toString() ?: ""
          )
          addInfo(
            title = "format.auxiliaryTrackType",
            value = format.auxiliaryTrackType.toString() ?: ""
          )
          addInfo(
            title = "format.averageBitrate",
            value = format.averageBitrate.toString() ?: ""
          )
          addInfo(
            title = "format.peakBitrate",
            value = format.peakBitrate.toString() ?: ""
          )
          addInfo(
            title = "format.bitrate",
            value = format.bitrate.toString() ?: ""
          )
          addInfo(
            title = "format.codecs",
            value = format.codecs ?: ""
          )
          addInfo(
            title = "format.containerMimeType",
            value = format.containerMimeType ?: ""
          )
          addInfo(
            title = "format.sampleMimeType",
            value = format.sampleMimeType ?: ""
          )
          addInfo(
            title = "format.maxInputSize",
            value = format.maxInputSize.toString() ?: ""
          )
          addInfo(
            title = "format.maxNumReorderSamples",
            value = format.maxNumReorderSamples.toString() ?: ""
          )
          addInfo(
            title = "format.initializationData",
            value = format.initializationData.toString() ?: ""
          )
          addInfo(
            title = "format.subsampleOffsetUs",
            value = format.subsampleOffsetUs.toString() ?: ""
          )
          addInfo(
            title = "format.hasPrerollSamples",
            value = format.hasPrerollSamples.toString() ?: ""
          )
          addInfo(
            title = "format.width",
            value = format.width.toString() ?: ""
          )
          addInfo(
            title = "format.height",
            value = format.height.toString() ?: ""
          )
          addInfo(
            title = "format.frameRate",
            value = format.frameRate.toString() ?: ""
          )
          addInfo(
            title = "format.rotationDegrees",
            value = format.rotationDegrees.toString() ?: ""
          )
          addInfo(
            title = "format.pixelWidthHeightRatio",
            value = format.pixelWidthHeightRatio.toString() ?: ""
          )
          addInfo(
            title = "format.projectionData",
            value = format.projectionData.toString() ?: ""
          )
          addInfo(
            title = "format.stereoMode",
            value = format.stereoMode.toString() ?: ""
          )
          addInfo(
            title = "format.maxSubLayers",
            value = format.maxSubLayers.toString() ?: ""
          )
          addInfo(
            title = "format.channelCount",
            value = format.channelCount.toString() ?: ""
          )
          addInfo(
            title = "format.sampleRate",
            value = format.sampleRate.toString() ?: ""
          )
          addInfo(
            title = "format.pcmEncoding",
            value = format.pcmEncoding.toString() ?: ""
          )
          addInfo(
            title = "format.encoderDelay",
            value = format.encoderDelay.toString() ?: ""
          )
          addInfo(
            title = "format.encoderPadding",
            value = format.encoderPadding.toString() ?: ""
          )
          addInfo(
            title = "format.accessibilityChannel",
            value = format.accessibilityChannel.toString() ?: ""
          )
          addInfo(
            title = "format.cueReplacementBehavior",
            value = format.cueReplacementBehavior.toString() ?: ""
          )
          addInfo(
            title = "format.tileCountHorizontal",
            value = format.tileCountHorizontal.toString() ?: ""
          )
          addInfo(
            title = "format.tileCountVertical",
            value = format.tileCountVertical.toString() ?: ""
          )
          addInfo(
            title = "format.cryptoType",
            value = format.cryptoType.toString() ?: ""
          )
        }
      }

      adapterHelper.setItems(newItems, this)
    }
  }
}