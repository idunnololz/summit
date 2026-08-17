package com.idunnololz.summit.localTracking.screen.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.FragmentLocalStatsListBinding
import com.idunnololz.summit.databinding.LocalStatsListItemBinding
import com.idunnololz.summit.localTracking.screen.LocalStatsFragment
import com.idunnololz.summit.localTracking.screen.LocalStatsFragment.LocalStatsAdapter.TitleType
import com.idunnololz.summit.localTracking.screen.list.LocalStatsListModel.Item
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.toBidiSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.util.toErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.R
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalStatsListFragment : BaseFragment<FragmentLocalStatsListBinding>() {

  enum class LocalStatsListType {
    FrequentedCommunities,
    FavoriteCommunities,
    UserInteractions,
  }

  private val args by navArgs<LocalStatsListFragmentArgs>()
  private val viewModel: LocalStatsListViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentLocalStatsListBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    requireSummitActivity().apply {
      setupForFragment<LocalStatsFragment>()
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
      insetViewAutomaticallyByPaddingAndNavUi(
        viewLifecycleOwner,
        binding.root,
        applyTopInset = false,
      )

      setupToolbar(
        binding.toolbar,
        when (args.type) {
          LocalStatsListType.FrequentedCommunities ->
            context.getString(com.idunnololz.summit.R.string.frequented_communities)
          LocalStatsListType.FavoriteCommunities ->
            context.getString(com.idunnololz.summit.R.string.favorite_communities)
          LocalStatsListType.UserInteractions ->
            context.getString(com.idunnololz.summit.R.string.user_interactions)
        }
      )
    }


    viewModel.loadStats(args.type)

    val context = requireContext()
    val adapter = LocalStatsListAdapter(
      context = context,
      fetchPerson = { viewModel.fetchPerson(it, false) },
    )

    binding.recyclerView.setHasFixedSize(true)
    binding.recyclerView.layoutManager = LinearLayoutManager(context)
    binding.recyclerView.adapter = adapter

    viewModel.data.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          binding.loadingView.showDefaultErrorMessageFor(it.error)
        }
        is StatefulData.Loading -> {
          binding.loadingView.showProgressBar()
        }
        is StatefulData.NotStarted -> {
          binding.loadingView.hideAll()
        }
        is StatefulData.Success -> {
          binding.loadingView.hideAll()

          adapter.data = it.data

        }
      }
    }
  }

  private class LocalStatsListAdapter(
    private val context: Context,
    val fetchPerson: (Long) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var data: LocalStatsListModel? = null
      set(value) {
        field = value

        refreshItems()
      }

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.CommunityStatItem ->
            old.communityRef == (new as Item.CommunityStatItem).communityRef
          is Item.PersonStatItem ->
            old.personId == (new as Item.PersonStatItem).personId
        }
      }
    ).apply {
      addItemType(Item.CommunityStatItem::class, LocalStatsListItemBinding::inflate) { item, b, h ->
        b.title.text = item.communityRef?.getLocalizedFullNameSpannable(context)
        b.stat.text = PrettyPrintUtils.defaultDecimalFormat.format(item.count)
      }
      addItemType(Item.PersonStatItem::class, LocalStatsListItemBinding::inflate) { item, b, h ->
        b.title.text = if (item.personId == null) {
          "null"
        } else if (item.personResult == null) {
          fetchPerson(item.personId)

          context.getString(com.idunnololz.summit.R.string.loading)
        } else {
          item.personResult.fold(
              {
                "${(it.display_name ?: it.name).toBidiSafe()}@${it.instance}"
              },
              {
                it.toErrorMessage(context)
              }
            )
        }

        b.stat.text = PrettyPrintUtils.defaultDecimalFormat.format(item.count)
      }
    }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(
      holder: RecyclerView.ViewHolder,
      position: Int,
    ) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    private fun refreshItems() {
      adapterHelper.setItems(newItems = data?.items ?: listOf(), adapter = this)
    }
  }
}