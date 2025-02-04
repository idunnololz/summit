package com.idunnololz.summit.lemmy.userTags

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentUserTagsBinding
import com.idunnololz.summit.databinding.ItemUserTagsUserTagBinding
import com.idunnololz.summit.spans.RoundedBackgroundSpan
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserTagsFragment :
    BaseFragment<FragmentUserTagsBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: UserTagsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentUserTagsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

                requireMainActivity().apply {
                    insetViewAutomaticallyByPaddingAndNavUi(
                        viewLifecycleOwner,
                        root,
                        applyTopInset = false,
                    )
                }
            }

            val adapter = UserTagsAdapter(
                context = context,
                onUserTagClick = {
                    AddOrEditUserTagDialogFragment.show(childFragmentManager, userTag = it)
                },
                onDeleteClick = {
                    OldAlertDialogFragment.Builder()
                        .setExtra("tag", it.personName)
                        .setMessage(getString(R.string.warn_delete_user_tag, it.personName))
                        .setPositiveButton(R.string.delete)
                        .setNegativeButton(R.string.cancel)
                        .createAndShow(childFragmentManager, "delete_user_tag")
                },
            )

            toolbar.setTitle(R.string.user_tags)
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(android.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.refresh(force = true)
            }

            fab.setOnClickListener {
                AddOrEditUserTagDialogFragment.show(childFragmentManager, null)
            }

            viewModel.model.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()
                    }
                    is StatefulData.Success -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()

                        adapter.setData(it.data)

                        if (it.data.userTags.isEmpty()) {
                            loadingView.showErrorText(
                                R.string.there_doesnt_seem_to_be_anything_here,
                            )
                        }
                    }
                }
            }
        }
    }

    private class UserTagsAdapter(
        private val context: Context,
        private val onUserTagClick: (UserTag) -> Unit,
        private val onDeleteClick: (UserTag) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class UserTagItem(
                val userTag: UserTag,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.UserTagItem -> {
                        old.userTag.personName == (new as Item.UserTagItem).userTag.personName
                    }
                }
            },
        ).apply {
            addItemType(
                clazz = Item.UserTagItem::class,
                inflateFn = ItemUserTagsUserTagBinding::inflate,
            ) { item, b, h ->
                b.title.text = item.userTag.personName
                b.body.text = buildSpannedString {
                    val tag = item.userTag.config

                    val s = length
                    append(tag.tagName)
                    val e = length

                    append(" ")

                    setSpan(
                        RoundedBackgroundSpan(tag.fillColor, tag.borderColor),
                        s,
                        e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                b.root.setOnClickListener {
                    onUserTagClick(item.userTag)
                }
                b.delete.setOnClickListener {
                    onDeleteClick(item.userTag)
                }
            }
        }

        private var data: UserTagsViewModel.Model? = null

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(data: UserTagsViewModel.Model) {
            this.data = data
            refresh()
        }

        private fun refresh() {
            val data = data
            val newItems = mutableListOf<Item>()

            data?.userTags?.mapTo(newItems) {
                Item.UserTagItem(it)
            }

            adapterHelper.setItems(newItems, this)
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        val personName = dialog.getExtra("tag")

        viewModel.deleteTag(personName)
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
