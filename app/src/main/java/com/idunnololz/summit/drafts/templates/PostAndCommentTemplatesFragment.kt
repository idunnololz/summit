package com.idunnololz.summit.drafts.templates

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.databinding.DialogChooseTemplateTypeBinding
import com.idunnololz.summit.databinding.EmptyItemBinding
import com.idunnololz.summit.databinding.FragmentPostAndCommentTemplatesBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ItemPostAndCommentTemplatePostBinding
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.drafts.addOrEditTemplate.AddOrEditCommentTemplateFragment
import com.idunnololz.summit.drafts.addOrEditTemplate.AddOrEditPostTemplateFragment
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item.CommentTemplateItem
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item.EmptyItem
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item.FooterItem
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item.HeaderItem
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item.PostTemplateItem
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.imageTintListCompat
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.you.YouFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostAndCommentTemplatesFragment  :
  BaseFragment<FragmentPostAndCommentTemplatesBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "DraftsDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager) {
      DraftsDialogFragment().apply {
      }.showAllowingStateLoss(fragmentManager, "PostsAndCommentsTemplatesFragment")
    }
  }

  private val viewModel: PostAndCommentTemplatesViewModel by viewModels()

  @Inject
  lateinit var accountAwareLemmyClient: AccountAwareLemmyClient

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentPostAndCommentTemplatesBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.load(force = true)
      }

      addTemplateFab.setOnClickListener {
        val dialogBinding = DialogChooseTemplateTypeBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
          .apply {
            setView(dialogBinding.root)
          }
          .show()

        dialogBinding.createPostTemplate.setOnClickListener {
          AddOrEditPostTemplateFragment.show(
            fragmentManager = childFragmentManager,
            instance = accountAwareLemmyClient.instance,
          )
          dialog.dismiss()
        }
        dialogBinding.createCommentTemplate.setOnClickListener {
          AddOrEditCommentTemplateFragment.show(
            fragmentManager = childFragmentManager,
            instance = accountAwareLemmyClient.instance,
          )
          dialog.dismiss()
        }
      }

      val adapter = PostAndCommentTemplatesAdapter(
        context = context,
        onPostTemplateClick = {
          AddOrEditPostTemplateFragment.show(
            fragmentManager = childFragmentManager,
            instance = accountAwareLemmyClient.instance,
            templateToEditId = it.entryId,
          )
        },
        onCommentTemplateClick = {
          AddOrEditCommentTemplateFragment.show(
            fragmentManager = childFragmentManager,
            instance = accountAwareLemmyClient.instance,
            templateToEditId = it.entryId,
          )
        }
      )
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      viewModel.model.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error<*> -> {
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading<*> -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted<*> -> {
            loadingView.hideAll()
            swipeRefreshLayout.isRefreshing = false
          }
          is StatefulData.Success -> {
            loadingView.hideAll()
            swipeRefreshLayout.isRefreshing = false

            adapter.data = it.data.items
          }
        }
      }
    }
  }

  private class PostAndCommentTemplatesAdapter(
    private val context: Context,
    private val onPostTemplateClick: (PostTemplateItem) -> Unit,
    private val onCommentTemplateClick: (CommentTemplateItem) -> Unit,
  ) : RecyclerView.Adapter<ViewHolder>() {

    private val adapterHelper = AdapterHelper<Item>(
      areItemsTheSame = { old, new ->
        old::class == new::class && when (old) {
          is CommentTemplateItem ->
            old.entryId == (new as CommentTemplateItem).entryId
          is PostTemplateItem ->
            old.entryId == (new as PostTemplateItem).entryId
          HeaderItem -> true
          EmptyItem -> true
          FooterItem -> true
        }
      }
    ).apply {
      addItemType(
        clazz = HeaderItem::class,
        inflateFn = ItemGenericHeaderBinding::inflate
      ) { _, _, _ -> }
      addItemType(
        clazz = FooterItem::class,
        inflateFn = GenericSpaceFooterItemBinding::inflate,
      ) { _, _, _ -> }
      addItemType(
        clazz = EmptyItem::class,
        inflateFn = EmptyItemBinding::inflate,
      ) { _, _, _ -> }
      addItemType(
        clazz = CommentTemplateItem::class,
        inflateFn = ItemPostAndCommentTemplatePostBinding::inflate,
      ) { item, b, h ->
        b.icon.apply {
          val iconColor = context.getColorCompat(R.color.style_amber)
          setImageResource(R.drawable.outline_comment_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        }
        b.title.text = if (item.commentTemplateData.name.isNullOrBlank()) {
          context.getString(R.string.no_name)
        } else {
          item.commentTemplateData.name
        }
        b.desc.text = item.description.ifBlank {
          context.getString(R.string.no_content)
        }
        b.root.setOnClickListener {
          onCommentTemplateClick(item)
        }
      }
      addItemType(
        clazz = PostTemplateItem::class,
        inflateFn = ItemPostAndCommentTemplatePostBinding::inflate,
      ) { item, b, h ->
        b.icon.apply {
          val iconColor = context.getColorCompat(R.color.style_blue)
          setImageResource(R.drawable.ic_post_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        }
        b.title.text = if (item.postTemplateData.name.isNullOrBlank()) {
          context.getString(R.string.no_name)
        } else {
          item.postTemplateData.name
        }
        b.desc.text = item.description.ifBlank {
          context.getString(R.string.no_content)
        }
        b.root.setOnClickListener {
          onPostTemplateClick(item)
        }
      }
    }

    var data: List<Item> = listOf()
      set(value) {
        field = value

        refreshItems()
      }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    private fun refreshItems() {
      adapterHelper.setItems(data, this)
    }
  }

}
