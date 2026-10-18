package com.idunnololz.summit.debug

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ActionDetailsItemTextFieldBinding
import com.idunnololz.summit.databinding.DialogFragmentDebugBinding
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostItem
import com.idunnololz.summit.lemmy.duplicatePostsDetector.DuplicatePostsDetector
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DebugDialogFragment : BaseDialogFragment<DialogFragmentDebugBinding>() {

  companion object {
    fun show(fragmentManager: FragmentManager) = DebugDialogFragment()
      .showAllowingStateLoss(fragmentManager, "DebugDialogFragment")
  }

  @Inject
  lateinit var duplicatePostsDetector: DuplicatePostsDetector

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentDebugBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()
    val duplicatePostsAdapter = DuplicatePostsAdapter(context)
    var duplicatePosts = emptyList<DuplicatePostItem>()

    fun updateItems() {
      val query = binding.searchEditText.text?.toString().orEmpty().trim()
      val filteredItems = if (query.isEmpty()) {
        duplicatePosts
      } else {
        duplicatePosts.filter { item ->
          item.key.contains(query, ignoreCase = true) ||
            item.postId.toString().contains(query)
        }
      }

      duplicatePostsAdapter.setItems(filteredItems)
      binding.emptyView.text = getString(
        if (query.isEmpty()) {
          R.string.debug_no_duplicate_posts
        } else {
          R.string.debug_no_matching_duplicate_posts
        },
      )
      binding.emptyView.isVisible = filteredItems.isEmpty()
    }

    with(binding) {
      setupToolbar(toolbar, getString(R.string.debug_duplicate_posts))

      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = duplicatePostsAdapter

      searchEditText.addTextChangedListener {
        updateItems()
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      duplicatePosts = duplicatePostsDetector.getCurrentItems().asReversed()
      updateItems()
    }
  }

  private class DuplicatePostsAdapter(
    private val context: Context,
  ) : Adapter<ViewHolder>() {
    private val adapterHelper = AdapterHelper<DuplicatePostItem>(
      areItemsTheSame = { old, new -> old.key == new.key },
    ).apply {
      addItemType(
        DuplicatePostItem::class,
        ActionDetailsItemTextFieldBinding::inflate,
      ) { item, binding, _ ->
        binding.label.text = context.getString(
          R.string.debug_post_id_format,
          item.postId,
        )
        binding.value.text = item.key
      }
    }

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    fun setItems(items: List<DuplicatePostItem>) {
      adapterHelper.setItems(items, this)
    }
  }
}
