package com.idunnololz.summit.settings.locale

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentChooseDefaultAppBinding
import com.idunnololz.summit.databinding.FragmentLocalePickerBottomSheetBinding
import com.idunnololz.summit.databinding.ItemAppChoiceBinding
import com.idunnololz.summit.databinding.ItemAppChoiceClearBinding
import com.idunnololz.summit.databinding.ItemLocalePickerChoiceBinding
import com.idunnololz.summit.settings.defaultApps.ChooseDefaultAppBottomSheetFragment
import com.idunnololz.summit.settings.defaultApps.ChooseDefaultAppBottomSheetFragment.Companion
import com.idunnololz.summit.settings.defaultApps.ChooseDefaultAppBottomSheetFragment.Result
import com.idunnololz.summit.settings.defaultApps.ChooseDefaultAppBottomSheetFragmentArgs
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getLocaleListFromXml
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import kotlinx.parcelize.Parcelize
import java.util.Locale

class LocalePickerBottomSheetFragment :
    BaseBottomSheetDialogFragment<FragmentLocalePickerBottomSheetBinding>(),
    FullscreenDialogFragment {

    companion object {

        const val RESULT_KEY = "LocalePickerBottomSheetFragment.result"
        const val REQUEST_KEY = "LocalePickerBottomSheetFragment.request"

        fun show(fragmentManager: FragmentManager) =
            LocalePickerBottomSheetFragment()
                .show(fragmentManager, "LocalePickerBottomSheetFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentLocalePickerBottomSheetBinding.inflate(
            inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            recyclerView.adapter = LocalesAdapter(
                context.getLocaleListFromXml(),
                {
                    if (it == null) {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.getEmptyLocaleList())
                    } else {
                        val appLocale: LocaleListCompat =
                            LocaleListCompat.forLanguageTags(it.toLanguageTag())
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                    setFragmentResult(
                        REQUEST_KEY,
                        Bundle()
                    )

                    dismiss()
                },
            )
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(false)
        }
    }

    private class LocalesAdapter(
        private val localeList: LocaleListCompat,
        private val onLocaleClick: (Locale?) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class LocaleItem(
                val locale: Locale
            ): Item

            data object ClearItem : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    Item.ClearItem -> true
                    is Item.LocaleItem ->
                        old.locale.toLanguageTag() ==
                            (new as Item.LocaleItem).locale.toLanguageTag()
                }
            }
        ).apply {
            addItemType(Item.LocaleItem::class, ItemLocalePickerChoiceBinding::inflate) { item, b, _ ->
                b.title.text = item.locale.displayLanguage

                b.root.setOnClickListener {
                    onLocaleClick(item.locale)
                }
            }
            addItemType(Item.ClearItem::class, ItemLocalePickerChoiceBinding::inflate) { _, b, _ ->
                b.title.setText(R.string.use_system_language)
                b.root.setOnClickListener {
                    onLocaleClick(null)
                }
            }
        }

        init {
            val newItems = mutableListOf<Item>()

            for (i in 0 until localeList.size()) {
                val locale = localeList.get(i) ?: continue
                newItems.add(Item.LocaleItem(locale))
            }
            newItems += Item.ClearItem
            adapterHelper.setItems(newItems, this)
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)
    }
}