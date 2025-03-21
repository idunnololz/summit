package com.idunnololz.summit.db.preview

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentTableDetailsBinding
import com.idunnololz.summit.databinding.ItemTableFooterBinding
import com.idunnololz.summit.databinding.ItemTableHeaderBinding
import com.idunnololz.summit.databinding.ItemTableRowBinding
import com.idunnololz.summit.databinding.ItemTableRowHeaderBinding
import com.idunnololz.summit.db.raw.TableRow
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TableDetailsDialogFragment :
    BaseDialogFragment<DialogFragmentTableDetailsBinding>() {

    private val viewModel: TableDetailsViewModel by viewModels()
    private val args: TableDetailsDialogFragmentArgs by navArgs()

    companion object {
        fun show(fragmentManager: FragmentManager, dbUri: Uri, tableName: String) {
            TableDetailsDialogFragment()
                .apply {
                    arguments = TableDetailsDialogFragmentArgs(
                        dbUri,
                        tableName,
                    ).toBundle()
                }
                .show(fragmentManager, "TableDetailsDialogFragment")
        }
    }

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

        setBinding(
            DialogFragmentTableDetailsBinding.inflate(
                inflater,
                container,
                false,
            ),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            toolbar.title = getString(R.string.table_details)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            val adapter = TableDetailsAdapter(context)

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = adapter

            viewModel.loadDbDetails(args.dbUri, args.tableName)

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

    private class TableDetailsAdapter(
        private val context: Context,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class HeaderItem(
                val tableName: String,
                val rowCount: Int,
            ) : Item

            data class RowHeaderItem(
                val columnNames: List<String>,
            ) : Item

            data class RowItem(
                val tableRow: TableRow,
            ) : Item

            data object FooterItem : Item
        }

        private var data: TableDetailsViewModel.Model? = null

        private val rowOddColor = context.getColorFromAttribute(
            com.google.android.material.R.attr.colorSurfaceContainerHigh,
        )
        private val rowEvenColor = context.getColorFromAttribute(
            com.google.android.material.R.attr.colorSurfaceContainerLow,
        )

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.HeaderItem -> true
                    is Item.RowHeaderItem -> true
                    is Item.RowItem ->
                        old.tableRow.primaryKey == (new as Item.RowItem).tableRow.primaryKey
                    Item.FooterItem -> true
                }
            },
        ).apply {
            addItemType(Item.HeaderItem::class, ItemTableHeaderBinding::inflate) { item, b, h ->
                b.tableName.text = item.tableName
                b.rowCount.text = PrettyPrintUtils.defaultDecimalFormat.format(item.rowCount)
            }
            addItemType(
                Item.RowHeaderItem::class,
                ItemTableRowHeaderBinding::inflate,
            ) { item, b, h ->
                if (b.root.childCount != item.columnNames.size) {
                    b.root.removeAllViews()

                    for (i in 0 until item.columnNames.size) {
                        b.root.addView(newColumnTextView(b.root.context))
                    }
                }

                for ((index, columnName) in item.columnNames.withIndex()) {
                    (b.root.getChildAt(index) as TextView).text = columnName
                }
            }
            addItemType(Item.RowItem::class, ItemTableRowBinding::inflate) { item, b, h ->
                val tableRow = item.tableRow
                val indexOdd = h.absoluteAdapterPosition % 2 == 1
                val primaryKeyPadding = if (item.tableRow.primaryKey == null) {
                    0
                } else {
                    1
                }
                if (b.root.childCount != tableRow.columns.size + primaryKeyPadding) {
                    b.root.removeAllViews()

                    for (i in 0 until tableRow.columns.size + primaryKeyPadding) {
                        b.root.addView(newColumnTextView(b.root.context))
                    }
                }

                if (indexOdd) {
                    b.root.setBackgroundColor(rowOddColor)
                } else {
                    b.root.setBackgroundColor(rowEvenColor)
                }

                (b.root.getChildAt(0) as TextView).text = tableRow.primaryKey
                for ((index, column) in tableRow.columns.withIndex()) {
                    (b.root.getChildAt(index + primaryKeyPadding) as TextView).text = column
                }

                b.root.setOnClickListener {
                    Utils.copyToClipboard(
                        context,
                        buildString {
                            append(item.tableRow.primaryKey)
                            append(",")
                            for ((index, c) in item.tableRow.columns.withIndex()) {
                                append(c)
                                if (index != item.tableRow.columns.lastIndex) {
                                    append(",")
                                }
                            }
                        },
                    )
                }
            }
            addItemType(Item.FooterItem::class, ItemTableFooterBinding::inflate) { item, b, h ->
            }
        }

        private fun newColumnTextView(context: Context) = AppCompatTextView(
            context,
            null,
            com.google.android.material.R.attr.textAppearanceBodySmall,
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = Utils.convertDpToPixel(4f).toInt()
                bottomMargin = Utils.convertDpToPixel(4f).toInt()
                marginStart = Utils.convertDpToPixel(4f).toInt()
                marginStart = Utils.convertDpToPixel(4f).toInt()
                marginEnd = Utils.convertDpToPixel(4f).toInt()
                weight = 1f
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        fun setData(data: TableDetailsViewModel.Model) {
            this.data = data

            refreshItems()
        }

        private fun refreshItems() {
            val data = data ?: return
            val newItems = mutableListOf<Item>()

            newItems += Item.HeaderItem(
                data.tableName,
                data.rowCount,
            )
            newItems += Item.RowHeaderItem(data.columnNames)

            data.rows.mapTo(newItems) { Item.RowItem(it) }

            newItems += Item.FooterItem

            adapterHelper.setItems(newItems, this)
        }
    }
}
