package com.idunnololz.summit.settings.translators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentTranslatorsBinding
import com.idunnololz.summit.databinding.TranslatorsLocaleItemBinding
import com.idunnololz.summit.databinding.TranslatorsTranslatorItemBinding
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TranslatorsFragment : BaseFragment<DialogFragmentTranslatorsBinding>() {

    private val viewModel: TranslatorsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentTranslatorsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.contentView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.translators)
        }

        val adapter = TranslatorsAdapter()

        with(binding) {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
        }

        viewModel.translatorStats.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error ->
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                is StatefulData.Loading ->
                    binding.loadingView.showProgressBar()
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()

                    adapter.setData(it.data)
                }
            }
        }

        viewModel.loadTranslatorsJsonIfNeeded()
    }

    private class TranslatorsAdapter : Adapter<ViewHolder>() {

        private sealed interface Item {

            data class LocaleItem(
                val locale: String,
            ) : Item

            data class TranslatorItem(
                val translatorName: String,
                val stringsTranslated: Int,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                when (old) {
                    is Item.LocaleItem ->
                        old.locale == (new as Item.LocaleItem).locale
                    is Item.TranslatorItem ->
                        old.translatorName == (new as Item.TranslatorItem).translatorName
                }
            },
        ).apply {
            addItemType(
                clazz = Item.TranslatorItem::class,
                inflateFn = TranslatorsTranslatorItemBinding::inflate,
            ) { item, b, h ->
                b.text.text = item.translatorName
            }
            addItemType(
                clazz = Item.LocaleItem::class,
                inflateFn = TranslatorsLocaleItemBinding::inflate,
            ) { item, b, h ->
                b.title.text = item.locale
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(data: Map<String, List<TranslationTranslatorStats>>) {
            val items = mutableListOf<Item>()

            for ((locale, translatorsStats) in data.entries) {
                if (translatorsStats.isEmpty()) continue

                items.add(Item.LocaleItem(locale))

                val translatorsSorted = translatorsStats.sortedByDescending { it.stringsTranslated }

                for (translatorStats in translatorsSorted) {
                    items.add(
                        Item.TranslatorItem(
                            translatorName = translatorStats.translatorName,
                            stringsTranslated = translatorStats.stringsTranslated.toInt(),
                        ),
                    )
                }
            }

            adapterHelper.setItems(items, this)
        }
    }
}
