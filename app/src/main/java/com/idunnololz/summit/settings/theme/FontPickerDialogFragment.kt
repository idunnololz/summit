package com.idunnololz.summit.settings.theme

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentColorSchemePickerBinding
import com.idunnololz.summit.databinding.FontItemBinding
import com.idunnololz.summit.preferences.FontId
import com.idunnololz.summit.preferences.FontIds
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preferences.toFontAsset
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import io.github.inflationx.calligraphy3.CalligraphyUtils
import javax.inject.Inject

@AndroidEntryPoint
class FontPickerDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentColorSchemePickerBinding>(),
    FullscreenDialogFragment {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentColorSchemePickerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = FontAdapter(context) {
                preferences.globalFont = it

                dismiss()
                themeManager.applyThemeFromPreferences()
                getMainActivity()?.recreate()
            }
        }
    }

    private class FontAdapter(
        private val context: Context,
        private val onFontSelected: (FontId) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        sealed interface Item {
            data class FontItem(
                val fontId: FontId,
                val name: String,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.FontItem ->
                        old.fontId == (new as Item.FontItem).fontId
                }
            },
        ).apply {
            addItemType(
                clazz = Item.FontItem::class,
                inflateFn = FontItemBinding::inflate,
            ) { item, b, h ->
                b.title.text = item.name
                b.sampleText.text = context.getString(R.string.font_sample_text)

                CalligraphyUtils.applyFontToTextView(context, b.title, item.fontId.toFontAsset())
                CalligraphyUtils.applyFontToTextView(context, b.sampleText, item.fontId.toFontAsset())

                b.root.setOnClickListener {
                    onFontSelected(item.fontId)
                }
            }
        }

        init {
            refreshItems()
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val newItems = listOf(
                Item.FontItem(
                    FontIds.Default,
                    context.getString(R.string._default),
                ),
                Item.FontItem(
                    FontIds.Roboto,
                    context.getString(R.string.roboto),
                ),
                Item.FontItem(
                    FontIds.RobotoSerif,
                    context.getString(R.string.roboto_serif),
                ),
                Item.FontItem(
                    FontIds.OpenSans,
                    context.getString(R.string.open_sans),
                ),
            )

            adapterHelper.setItems(newItems, this)
        }
    }
}