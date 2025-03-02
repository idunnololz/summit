package com.idunnololz.summit.util.colorPicker.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ItemColorPickerHistoryBinding
import com.idunnololz.summit.databinding.ViewColorPickerHistoryBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.util.colorPicker.ColorUtils
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.colorPicker.utils.ColorPickerContainer
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class ColorPickerHistoryView(
    context: Context,
    private val globalStateStorage: GlobalStateStorage,
) : ConstraintLayout(context), ColorPicker, ColorPickerContainer {

    private var listener: OnColorPickedListener? = null

    init {
        val b = ViewColorPickerHistoryBinding.inflate(LayoutInflater.from(context), this)

        val colors = globalStateStorage.colorPickerHistory?.split(",") ?: listOf()

        b.recyclerView.setHasFixedSize(true)
        b.recyclerView.layoutManager = LinearLayoutManager(context)
        b.recyclerView.adapter = ColorsAdapter(colors) {
            listener?.onColorPicked(this, it)
        }
    }

    override fun setColor(color: Int, animate: Boolean) {}

    override fun setListener(listener: OnColorPickedListener?) {
        this.listener = listener
    }

    override val color: Int
        get() = 0
    override val name: String
        get() = context.getString(R.string.history)
    override val view: View
        get() = this
    override val colorPicker: ColorPicker
        get() = this
    override val rootView2: View
        get() = this

    private class ColorsAdapter(
        private val data: List<String>,
        private val onColorSelected: (Int) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            class ColorItem(
                val colorString: String,
                val color: Int,
                val textColor: Int,
            ): Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.ColorItem ->
                        old.color == (new as Item.ColorItem).color
                }
            }
        ).apply {
            addItemType(Item.ColorItem::class, ItemColorPickerHistoryBinding::inflate) { item, b, h ->
                b.card.setCardBackgroundColor(item.color)
                b.text.text = item.colorString
                b.text.setTextColor(item.textColor)
                b.card.setOnClickListener {
                    onColorSelected(item.color)
                }
            }
        }

        init {
            refreshData()
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshData() {
            val items = mutableListOf<Item>()

            data.forEach {
                try {
                    val color = Color.parseColor(it)
                    val textColor = if (ColorUtils.isColorDark(
                            ColorUtils.withBackground(
                                color,
                                Color.WHITE,
                            ),
                        )
                    ) Color.WHITE else Color.BLACK

                    items.add(
                        Item.ColorItem(
                            colorString = it,
                            color = color,
                            textColor = textColor,
                        )
                    )
                } catch (e: Exception) {
                    // do nothing
                }
            }

            adapterHelper.setItems(items, this)
        }

    }
}