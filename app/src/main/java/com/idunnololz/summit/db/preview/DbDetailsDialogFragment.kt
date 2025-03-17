package com.idunnololz.summit.db.preview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentDbDetailsBinding
import com.idunnololz.summit.databinding.ItemDbHeaderBinding
import com.idunnololz.summit.databinding.ItemDbTableBinding
import com.idunnololz.summit.databinding.ItemTableFooterBinding
import com.idunnololz.summit.db.raw.TableInfo
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import java.util.Locale

class DbDetailsDialogFragment :
    BaseDialogFragment<DialogFragmentDbDetailsBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            dbUri: Uri,
            title: String? = null,
            tableNames: List<String>? = null,
        ) {
            DbDetailsDialogFragment()
                .apply {
                    arguments = DbDetailsDialogFragmentArgs(
                        dbUri,
                        title,
                        tableNames?.joinToString(separator = ","),
                    ).toBundle()
                }
                .show(fragmentManager, "DbDetailsDialogFragment")
        }
    }

    private val args: DbDetailsDialogFragmentArgs by navArgs()
    private val viewModel: DbDetailsViewModel by viewModels()

    override fun onStart() {
        super.onStart()

        setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentDbDetailsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            toolbar.title = args.title ?: getString(R.string.database_details)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            val adapter = DatabaseInfoAdapter(context) {
                TableDetailsDialogFragment.show(
                    parentFragmentManager,
                    args.databaseUri,
                    it.tableName,
                )
            }
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = adapter

            viewModel.loadDbDetails(
                args.databaseUri,
                args.tableNames?.let {
                    it.split(",").toSet()
                },
            )
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
        }
    }

    private class DatabaseInfoAdapter(
        private val context: Context,
        private val onTableClick: (TableInfo) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            data class HeaderItem(
                val dbName: String,
            ) : Item

            data class TableItem(
                @DrawableRes val icon: Int,
                val name: String,
                val description: String,
                val tableInfo: TableInfo,
            ) : Item

            data object FooterItem : Item
        }

        private var data: DbDetailsViewModel.Model? = null

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    is Item.HeaderItem -> true
                    is Item.TableItem ->
                        old.tableInfo.tableName == (new as Item.TableItem).tableInfo.tableName
                    is Item.FooterItem -> true
                }
            },
        ).apply {
            addItemType(Item.HeaderItem::class, ItemDbHeaderBinding::inflate) { item, b, h ->
                b.databaseName.text = item.dbName
            }
            addItemType(Item.TableItem::class, ItemDbTableBinding::inflate) { item, b, h ->
                b.icon.setImageResource(item.icon)
                b.title.text = item.name
                b.body.text = item.description
                b.subtitle.text = context.getString(
                    R.string.row_count_format,
                    PrettyPrintUtils.defaultDecimalFormat.format(item.tableInfo.rowCount),
                )

                b.root.setOnClickListener {
                    onTableClick(item.tableInfo)
                }
            }
            addItemType(Item.FooterItem::class, ItemTableFooterBinding::inflate) { item, b, h ->
            }
        }

        override fun getItemViewType(position: Int) = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(newData: DbDetailsViewModel.Model) {
            data = newData

            refreshItems()
        }

        private fun refreshItems() {
            val data = data ?: return

            val newItems = mutableListOf<Item>()

            newItems += Item.HeaderItem(
                data.databaseName ?: context.getString(R.string.unknown),
            )

            data.tablesInfo.mapTo(newItems) {
                val icon: Int
                val name: String
                val desc: String

                when (it.tableName.lowercase(Locale.US)) {
                    "android_metadata" -> {
                        icon = R.drawable.baseline_miscellaneous_services_24
                        name = "Android metadata"
                        desc = context.getString(R.string.android_metadata_desc)
                    }
                    "sqlite_sequence" -> {
                        icon = R.drawable.baseline_miscellaneous_services_24
                        name = "SQLite sequence"
                        desc = context.getString(R.string.system_table_desc)
                    }
                    "room_master_table" -> {
                        icon = R.drawable.baseline_miscellaneous_services_24
                        name = "Room master table"
                        desc = context.getString(R.string.system_table_desc)
                    }
                    "user_communities" -> {
                        icon = R.drawable.ic_lemmy_outline_community_icon_24
                        name = context.getString(R.string.user_communities)
                        desc = context.getString(R.string.user_communities_table_desc)
                    }
                    "history" -> {
                        icon = R.drawable.baseline_history_24
                        name = context.getString(R.string.history)
                        desc = context.getString(R.string.history_table_desc)
                    }
                    "account" -> {
                        icon = R.drawable.outline_account_circle_24
                        name = context.getString(R.string.account)
                        desc = context.getString(R.string.account_table_desc)
                    }
                    "lemmy_actions" -> {
                        icon = R.drawable.outline_play_arrow_24
                        name = context.getString(R.string.user_actions)
                        desc = context.getString(R.string.user_actions_table_desc)
                    }
                    "lemmy_failed_actions" -> {
                        icon = R.drawable.outline_play_arrow_24
                        name = context.getString(R.string.failed_actions)
                        desc = context.getString(R.string.failed_actions_table_desc)
                    }
                    "lemmy_completed_actions" -> {
                        icon = R.drawable.outline_play_arrow_24
                        name = context.getString(R.string.completed_actions)
                        desc = context.getString(R.string.completed_actions_table_desc)
                    }
                    "account_info" -> {
                        icon = R.drawable.outline_account_circle_24
                        name = context.getString(R.string.account_info)
                        desc = context.getString(R.string.account_info_table_desc)
                    }
                    "hidden_posts" -> {
                        icon = R.drawable.baseline_hide_24
                        name = context.getString(R.string.hidden_posts)
                        desc = context.getString(R.string.hidden_posts_table_desc)
                    }
                    "content_filters" -> {
                        icon = R.drawable.baseline_filter_list_24
                        name = context.getString(R.string.content_filters)
                        desc = context.getString(R.string.content_filters_table_desc)
                    }
                    "drafts" -> {
                        icon = R.drawable.ic_draft_24
                        name = context.getString(R.string.drafts)
                        desc = context.getString(R.string.drafts_table_desc)
                    }
                    "inbox_entries" -> {
                        icon = R.drawable.outline_inbox_24
                        name = context.getString(R.string.inbox)
                        desc = context.getString(R.string.inbox_table_desc)
                    }
                    "conversation_entries" -> {
                        icon = R.drawable.outline_forum_24
                        name = context.getString(R.string.messages)
                        desc = context.getString(R.string.messages_table_desc)
                    }
                    "read_posts" -> {
                        icon = R.drawable.baseline_check_24
                        name = context.getString(R.string.read_posts)
                        desc = context.getString(R.string.read_posts_table_desc)
                    }
                    "text_emojis" -> {
                        icon = R.drawable.outline_emoji_emotions_24
                        name = context.getString(R.string.text_emojis)
                        desc = context.getString(R.string.text_emojis_table_desc)
                    }
                    "user_tags" -> {
                        icon = R.drawable.outline_label_24
                        name = context.getString(R.string.user_tags)
                        desc = context.getString(R.string.user_tags_table_desc)
                    }
                    else -> {
                        icon = R.drawable.baseline_question_mark_24
                        name = context.getString(R.string.unknown_table)
                        desc = context.getString(R.string.unknown_table_desc)
                    }
                }

                Item.TableItem(
                    icon = icon,
                    name = name,
                    description = desc,
                    tableInfo = it,
                )
            }

            newItems += Item.FooterItem

            adapterHelper.setItems(newItems, this)
        }
    }
}
