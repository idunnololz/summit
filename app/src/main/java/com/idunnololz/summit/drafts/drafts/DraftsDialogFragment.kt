package com.idunnololz.summit.drafts.drafts

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.DialogFragmentDraftsBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.addOrEditTemplate.AddOrEditCommentTemplateFragment
import com.idunnololz.summit.drafts.addOrEditTemplate.AddOrEditPostTemplateFragment
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class DraftsDialogFragment :
  BaseDialogFragment<DialogFragmentDraftsBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "DraftsDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager, draftType: Int) {
      DraftsDialogFragment().apply {
        arguments = DraftsDialogFragmentArgs(draftType).toBundle()
      }.showAllowingStateLoss(fragmentManager, "DraftsDialogFragment")
    }
  }

  @Parcelize
  data class Result(
    val draft: DraftEntry?,
    val commentTemplate: TemplateData.CommentTemplateData?,
    val postTemplate: TemplateData.PostTemplateData?,
  ) : Parcelable

  private val args by navArgs<DraftsDialogFragmentArgs>()

  private val viewModel: DraftsViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val deleteAllDialogLauncher = newAlertDialogLauncher("delete_all") {
    if (it.isOk) {
      viewModel.deleteAll(args.draftType)
    }
  }
  private val deleteDraftDialogLauncher = newAlertDialogLauncher("delete") {
    if (it.isOk) {
      it.extras?.getLong("draft_id")?.let { draftId ->
        viewModel.deleteDraft(draftId)
      }
    }
  }
  private val deleteSelectedDialogLauncher = newAlertDialogLauncher("delete_selected") {
    if (it.isOk) {
      viewModel.deleteAllSelectedEntries()
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.let { window ->
      WindowCompat.setDecorFitsSystemWindows(window, false)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentDraftsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    val onBackPressedCallback = object : OnBackPressedCallback(
      enabled = viewModel.model.value?.isInSelectMode == true,
    ) {
      override fun handleOnBackPressed() {
        viewModel.isInSelectMode = false
      }
    }
    requireSummitActivity().onBackPressedDispatcher
      .addCallback(
        viewLifecycleOwner,
        onBackPressedCallback,
      )

    viewModel.draftType = args.draftType

    with(binding) {
      requireMainActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, root)
      }

      setupToolbar(toolbar, "")

      viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
          viewModel.filter.collect {
            updateToolbarMenu(it)
          }
        }
      }

      setupRecyclerView(recyclerViewDrafts, Filter.Drafts)
      setupRecyclerView(recyclerViewTemplates, Filter.Templates)

      val pages = listOf(recyclerViewDrafts, recyclerViewTemplates)
      contentContainer.removeAllViews()
      contentContainer.adapter = object : PagerAdapter() {
        override fun getCount(): Int = pages.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any =
          pages[position].also(container::addView)

        override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
          container.removeView(item as View)
        }

        override fun isViewFromObject(view: View, item: Any): Boolean = view === item
      }
      contentContainer.addOnPageChangeListener(
        object : ViewPager.SimpleOnPageChangeListener() {
          override fun onPageSelected(position: Int) {
            viewModel.filter.value = when (position) {
              0 -> Filter.Drafts
              else -> Filter.Templates
            }
          }
        },
      )

      viewModel.model.observe(viewLifecycleOwner) { model ->
        val page = when (model.filter) {
          Filter.Drafts -> 0
          Filter.Templates -> 1
        }
        if (contentContainer.currentItem != page) {
          contentContainer.setCurrentItem(page, true)
        }

        buttonGroup.check(
          when (model.filter) {
            Filter.Drafts -> R.id.drafts_button
            Filter.Templates -> R.id.templates_button
          },
        )

        onBackPressedCallback.isEnabled = model.isInSelectMode

        if (model.isInSelectMode) {
          if (!deleteFab.isShown) {
            deleteFab.show()
          }
        } else {
          if (deleteFab.isShown) {
            deleteFab.hide()
          }
        }
      }

      buttonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
        if (!isChecked) return@addOnButtonCheckedListener

        when (buttonGroup.checkedButtonId) {
          R.id.drafts_button -> {
            viewModel.filter.value = Filter.Drafts
          }
          R.id.templates_button -> {
            viewModel.filter.value = Filter.Templates
          }
        }
      }

      deleteFab.setOnClickListener {
        deleteSelectedDialogLauncher.launchDialog {
          message = resources.getQuantityString(
            R.plurals.warn_delete_drafts_format,
            viewModel.selectedItemsCount,
            PrettyPrintUtils.defaultDecimalFormat.format(viewModel.selectedItemsCount),
          )
          positionButtonResId = R.string.delete
          negativeButtonResId = R.string.cancel
        }
      }
    }
  }

  private fun setupRecyclerView(recyclerView: RecyclerView, filter: Filter) {
    val context = recyclerView.context

    val adapter = DraftsAdapter(
      context = context,
      filter = filter,
      onDraftClick = {
        // Convert the draft data to the correct type (eg. if comment draft was requested
        // but the draft selected was a post then convert the post to a comment)
        val draft = when (args.draftType) {
          DraftTypes.Post -> {
            when (it.data) {
              is DraftData.CommentDraftData ->
                it.copy(
                  id = 0L, // prevent overwriting the original
                  data = DraftData.PostDraftData(
                    originalPost = null,
                    name = null,
                    body = it.data.content,
                    url = null,
                    isNsfw = false,
                    accountId = it.data.accountId,
                    accountInstance = it.data.accountInstance,
                    targetCommunityFullName = "",
                    languageId = it.data.languageId,
                  ),
                )

              is DraftData.PostDraftData -> it
              is DraftData.MessageDraftData -> null
              null -> null
            }
          }

          DraftTypes.Comment -> {
            when (it.data) {
              is DraftData.CommentDraftData -> it
              is DraftData.PostDraftData ->
                it.copy(
                  id = 0L, // prevent overwriting the original
                  data = DraftData.CommentDraftData(
                    originalComment = null,
                    postRef = null,
                    parentCommentId = null,
                    content = it.data.body ?: "",
                    accountId = it.data.accountId,
                    accountInstance = it.data.accountInstance,
                    languageId = it.data.languageId,
                  ),
                )

              is DraftData.MessageDraftData -> null
              null -> null
            }
          }

          else -> null
        }

        setFragmentResult(
          REQUEST_KEY,
          Bundle().apply {
            putParcelable(
              REQUEST_KEY_RESULT,
              Result(
                draft = draft,
                commentTemplate = null,
                postTemplate = null,
              ),
            )
          },
        )
        dismiss()
      },
      onDeleteClick = {
        deleteDraftDialogLauncher.launchDialog {
          messageResId = R.string.warn_delete_draft
          positionButtonResId = R.string.delete
          negativeButtonResId = R.string.cancel
          extras.putLong("draft_id", it.id)
        }
      },
      onPostTemplateClick = {
        setFragmentResult(
          REQUEST_KEY,
          Bundle().apply {
            putParcelable(
              REQUEST_KEY_RESULT,
              Result(
                draft = null,
                commentTemplate = null,
                postTemplate = it.postTemplateData,
              ),
            )
          },
        )
        dismiss()
      },
      onCommentTemplateClick = {
        setFragmentResult(
          REQUEST_KEY,
          Bundle().apply {
            putParcelable(
              REQUEST_KEY_RESULT,
              Result(
                draft = null,
                commentTemplate = it.commentTemplateData,
                postTemplate = null,
              ),
            )
          },
        )
        dismiss()
      },
      onEditPostTemplateClick = {
        AddOrEditPostTemplateFragment.show(
          fragmentManager = childFragmentManager,
          instance = viewModel.apiInstance,
          templateToEditId = it.entryId,
        )
      },
      onEditCommentTemplateClick = {
        AddOrEditCommentTemplateFragment.show(
          fragmentManager = childFragmentManager,
          instance = viewModel.apiInstance,
          templateToEditId = it.entryId,
        )
      },
      onStartSelectionMode = {
        viewModel.isInSelectMode = true
      },
      onItemSelected = { itemId, isSelected ->
        viewModel.markItemAsSelected(itemId, isSelected)
      },
    )
    val layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = adapter
    recyclerView.layoutManager = layoutManager
    recyclerView.setup(animationsHelper)
    recyclerView.setHasFixedSize(true)

    fun fetchPageIfLoadItem(position: Int) {
      (adapter.model.draftItems.getOrNull(position) as? ViewModelItem.LoadingItem)
        ?.let {
          viewModel.loadData()
        }
    }

    fun checkIfFetchNeeded() {
      val firstPos = layoutManager.findFirstVisibleItemPosition()
      val lastPos = layoutManager.findLastVisibleItemPosition()

      for (i in (firstPos - 1)..(lastPos + 1)) {
        fetchPageIfLoadItem(i)
      }
    }

    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(recyclerView, dx, dy)

          checkIfFetchNeeded()
        }
      },
    )

    viewModel.model.observe(viewLifecycleOwner) { model ->
      adapter.setModel(model) {
        checkIfFetchNeeded()
      }
    }
  }

  fun updateToolbarMenu(filter: Filter) {
    if (!isBindingAvailable()) {
      return
    }

    with(binding) {
      toolbar.menu.clear()

      when (filter) {
        Filter.Drafts -> {
          toolbar.inflateMenu(R.menu.menu_drafts)
          toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.delete_all -> {
                deleteAllDialogLauncher.launchDialog {
                  messageResId = R.string.warn_delete_all_drafts
                  positionButtonResId = R.string.delete_all
                  negativeButtonResId = R.string.cancel
                }
                true
              }
              R.id.show_all_drafts -> {
                if (viewModel.draftType == null) {
                  viewModel.draftType = args.draftType
                  it.setIcon(R.drawable.baseline_filter_list_off_24)
                } else {
                  viewModel.draftType = null
                  it.setIcon(R.drawable.baseline_filter_list_24)
                }
                viewModel.loadData(force = true)
                true
              }
              else -> false
            }
          }
        }
        Filter.Templates -> {
        }
      }
    }
  }
}
