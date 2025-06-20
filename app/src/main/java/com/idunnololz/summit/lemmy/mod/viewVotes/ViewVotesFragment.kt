package com.idunnololz.summit.lemmy.mod.viewVotes

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.VoteView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentViewVotesBinding
import com.idunnololz.summit.databinding.InboxListLoaderItemBinding
import com.idunnololz.summit.databinding.ItemInboxHeaderBinding
import com.idunnololz.summit.databinding.VoteItemBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.misc.DisplayInstanceOptions
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.appendLink
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewVotesFragment :
  BaseFragment<FragmentViewVotesBinding>(),
  FullscreenDialogFragment {

  companion object {
  }

  private val args: ViewVotesFragmentArgs by navArgs()
  private val viewModel: ViewVotesViewModel by viewModels()

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var preferences: Preferences

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentViewVotesBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.loadVotes(args.postId.toInt(), args.commentId.toInt())

    with(binding) {
      requireSummitActivity().apply {
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        insetViewAutomaticallyByPaddingAndNavUi(
          viewLifecycleOwner,
          binding.recyclerView,
          applyTopInset = false,
        )
      }

      setupToolbar(toolbar, getString(R.string.votes))

      val adapter = VotesAdapter(
        instance = viewModel.apiInstance,
        avatarHelper = avatarHelper,
        preferences = preferences,
        onPersonClick = {
          getMainActivity()?.launchPage(it)
        },
        onPersonLongClick = {
          getMainActivity()?.showMoreLinkOptions(LinkUtils.getLinkForPerson(it), null)
        },
      )

      recyclerView.setHasFixedSize(true)
      recyclerView.setup(animationsHelper)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter
      recyclerView.addOnScrollListener(
        object : OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
              ?: return

            if (layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1) {
              viewModel.loadMoreIfNeeded()
            }
          }
        },
      )

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.resetAndLoad()
      }

      viewModel.votesModel.observe(viewLifecycleOwner) {
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
            swipeRefreshLayout.isRefreshing = false

            adapter.data = it.data
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

    setupForFragment<ViewVotesFragment>()
  }

  private class VotesAdapter(
    private val instance: String,
    private val avatarHelper: AvatarHelper,
    private val preferences: Preferences,
    private val onPersonClick: (PersonRef) -> Unit,
    private val onPersonLongClick: (PersonRef.PersonRefComplete) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
      data object HeaderItem : Item

      class VoteItem(
        val voteView: VoteView,
      ) : Item

      data class LoaderItem(
        val state: StatefulData<Unit>,
      ) : Item
    }

    var data: VotesModel? = null
      set(value) {
        field = value
        refreshItems()
      }

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.VoteItem -> old.voteView.creator.id == (new as Item.VoteItem).voteView.creator.id
          Item.HeaderItem -> true
          is Item.LoaderItem -> true
        }
      },
    ).apply {
      addItemType(Item.HeaderItem::class, ItemInboxHeaderBinding::inflate) { item, b, _ -> }
      addItemType(Item.VoteItem::class, VoteItemBinding::inflate) { item, b, h ->
        val person = item.voteView.creator

        avatarHelper.loadAvatar(b.icon, person)

        val postInstance = person.instance
        val displayFullName = when (preferences.displayInstanceStyle) {
          DisplayInstanceOptions.NeverDisplayInstance -> {
            false
          }
          DisplayInstanceOptions.OnlyDisplayNonLocalInstances -> {
            instance != postInstance
          }
          DisplayInstanceOptions.AlwaysDisplayInstance -> {
            true
          }
          else -> false
        }

        val sb = SpannableStringBuilder()
        if (displayFullName) {
          sb.appendNameWithInstance(
            context = b.root.context,
            name = if (preferences.preferUserDisplayName) {
              person.display_name
                ?: person.name
            } else {
              person.name
            },
            instance = postInstance,
            url = LinkUtils.getLinkForPerson(person.toPersonRef()),
          )
        } else {
          sb.appendLink(
            if (preferences.preferUserDisplayName) {
              person.display_name
                ?: person.name
            } else {
              person.name
            },
            url = LinkUtils.getLinkForPerson(person.toPersonRef()),
          )
        }
        b.text.text = sb

        if (item.voteView.score > 0L) {
          ImageViewCompat.setImageTintList(
            b.voteDirection,
            ColorStateList.valueOf(preferences.upvoteColor),
          )
          b.voteDirection.setImageResource(R.drawable.baseline_arrow_upward_24)
        } else {
          ImageViewCompat.setImageTintList(
            b.voteDirection,
            ColorStateList.valueOf(preferences.downvoteColor),
          )
          b.voteDirection.setImageResource(R.drawable.baseline_arrow_downward_24)
        }

        b.root.setOnClickListener {
          onPersonClick(person.toPersonRef())
        }
        b.root.setOnLongClickListener {
          onPersonLongClick(person.toPersonRef())
          true
        }
      }
      addItemType(Item.LoaderItem::class, InboxListLoaderItemBinding::inflate) { item, b, _ ->
        when (val state = item.state) {
          is StatefulData.Error -> {
            b.loadingView.showDefaultErrorMessageFor(state.error)
          }
          is StatefulData.Loading ->
            b.loadingView.showProgressBar()
          is StatefulData.NotStarted ->
            b.loadingView.showProgressBar()
          is StatefulData.Success ->
            b.loadingView.showErrorText(R.string.no_more_items)
        }
      }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int = adapterHelper.itemCount

    private fun refreshItems() {
      val data = data ?: return
      val newItems = mutableListOf<Item>()

      newItems.add(Item.HeaderItem)
      data.pages.flatMapTo(newItems) {
        it.items.map { Item.VoteItem(it) }
      }

      if (data.pages.lastOrNull()?.hasMore == true) {
        newItems.add(Item.LoaderItem(StatefulData.Loading()))
      } else {
        newItems.add(Item.LoaderItem(StatefulData.Success(Unit)))
      }

      adapterHelper.setItems(newItems, this)
    }
  }
}
