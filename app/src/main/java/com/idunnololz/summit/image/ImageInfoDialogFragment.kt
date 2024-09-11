package com.idunnololz.summit.image

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentImageInfoBinding
import com.idunnololz.summit.databinding.ImageInfoInfoItemBinding
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

@AndroidEntryPoint
class ImageInfoDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentImageInfoBinding>(),
    FullscreenDialogFragment {

    companion object {
        const val REQUEST_KEY = "DraftsDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager, url: String) {
            ImageInfoDialogFragment().apply {
                arguments = ImageInfoDialogFragmentArgs(url).toBundle()
            }.showAllowingStateLoss(fragmentManager, "ImageInfoDialogFragment")
        }
    }

    private val args by navArgs<ImageInfoDialogFragmentArgs>()

    private val viewModel: ImageInfoViewModel by viewModels()

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentImageInfoBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
//            toolbar.title = getString(R.string.image_info)
//            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
//            toolbar.setNavigationOnClickListener {
//                dismiss()
//            }
//            toolbar.setNavigationIconTint(
//                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
//            )

            val adapter = ImageInfoAdapter(context)

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
            recyclerView.setup(animationsHelper)
            loadingView.setOnRefreshClickListener {
                viewModel.loadImageInfo(args.url, force = true)
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

                        adapter.setItems(it.data.items) {
                            recyclerView.scrollToPosition(0)
                        }
                    }
                }
            }

            viewModel.loadImageInfo(args.url)
        }
    }

    private class ImageInfoAdapter(
        private val context: Context,
    ) : Adapter<ViewHolder>() {

        private var items: List<ImageInfoModel.Item> = listOf()

        private val adapterHelper = AdapterHelper<ImageInfoModel.Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    is ImageInfoModel.InfoItem ->
                        old.title == (new as ImageInfoModel.InfoItem).title
                }
            },
        ).apply {
            addItemType(
                ImageInfoModel.InfoItem::class,
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

        fun setItems(items: List<ImageInfoModel.Item>, cb: () -> Unit) {
            this.items = items

            refreshItems(cb)
        }

        private fun refreshItems(cb: () -> Unit) {
            adapterHelper.setItems(items, this, cb)
        }
    }
}
