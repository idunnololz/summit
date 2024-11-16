package com.idunnololz.summit.lemmy.languageSelect

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Language
import com.idunnololz.summit.api.dto.LanguageId
import com.idunnololz.summit.databinding.DialogFragmentLanguageSelectBinding
import com.idunnololz.summit.databinding.LanguageItemBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

class LanguageSelectDialogFragment :
    BaseDialogFragment<DialogFragmentLanguageSelectBinding>(),
    FullscreenDialogFragment {

    companion object {
        const val REQUEST_KEY = "LanguageSelectDialogFragment_req"
        const val REQUEST_RESULT = "result"

        fun show(
            languages: List<Language>,
            selectedLanguages: List<LanguageId>,
            fragmentManager: FragmentManager,
        ) {
            LanguageSelectDialogFragment()
                .apply {
                    arguments = LanguageSelectDialogFragmentArgs(
                        languages.toTypedArray(),
                        selectedLanguages.toIntArray(),
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "aaa")
        }
    }

    @Parcelize
    data class Result(
        val selectedLanguages: List<LanguageId>,
    ) : Parcelable

    private val args: LanguageSelectDialogFragmentArgs by navArgs()

    private var adapter: LanguagesAdapter? = null

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentLanguageSelectBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        val adapter = LanguagesAdapter().apply {
            setData(args.languages.toList(), args.selectedLanguages.toList())
        }

        this.adapter = adapter

        with(binding) {
            toolbar.title = getString(R.string.languages)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(android.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
            toolbar.inflateMenu(R.menu.language_select)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.check_all -> {
                        adapter.checkAll()
                    }
                    R.id.uncheck_all -> {
                        adapter.uncheckAll()
                    }
                }

                updateMenu(adapter)
                true
            }

            updateMenu(adapter)

            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }
    }

    private fun updateMenu(adapter: LanguagesAdapter) {
        if (adapter.isAllSelected()) {
            binding.toolbar.menu.findItem(R.id.check_all).isVisible = false
            binding.toolbar.menu.findItem(R.id.uncheck_all).isVisible = true
        } else {
            binding.toolbar.menu.findItem(R.id.check_all).isVisible = true
            binding.toolbar.menu.findItem(R.id.uncheck_all).isVisible = false
        }
    }

    override fun onDestroyView() {
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                REQUEST_RESULT to Result(
                    adapter?.selectedLanguages?.toList()
                        ?: listOf(),
                ),
            ),
        )

        super.onDestroyView()
    }

    private fun onBackPressed() {
        dismiss()
    }

    private class LanguagesAdapter : Adapter<ViewHolder>() {

        private sealed interface Item {
            class LanguageItem(
                val name: String,
                val languageId: LanguageId,
                val isChecked: Boolean,
            ) : Item
        }

        private var languages: List<Language> = listOf()
        var selectedLanguages: MutableSet<LanguageId> = mutableSetOf()
            private set

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                when (old) {
                    is Item.LanguageItem ->
                        old.languageId == (new as Item.LanguageItem).languageId
                }
            },
        ).apply {
            addItemType(Item.LanguageItem::class, LanguageItemBinding::inflate) { item, b, h ->
                b.checkbox.isChecked = item.isChecked
                b.checkbox.text = item.name

                b.checkbox.setOnCheckedChangeListener { compoundButton, b ->
                    if (b != item.isChecked) {
                        onCheckChanged(item.languageId, b)
                    }
                }
            }
        }

        private fun onCheckChanged(languageId: LanguageId, b: Boolean) {
            if (b) {
                selectedLanguages.add(languageId)
            } else {
                selectedLanguages.remove(languageId)
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(languages: List<Language>, selectedLanguages: List<LanguageId>) {
            this.languages = languages
            this.selectedLanguages = selectedLanguages.toMutableSet()
            refreshItems()
        }

        fun refreshItems() {
            val newItems = languages.map {
                Item.LanguageItem(
                    it.name,
                    it.id,
                    selectedLanguages.contains(it.id),
                )
            }

            adapterHelper.setItems(newItems, this)
        }

        fun checkAll() {
            languages.forEach {
                selectedLanguages.add(it.id)
            }
            selectedLanguages.remove(languages.find { it.code == "und" }?.id)

            refreshItems()
        }

        fun uncheckAll() {
            selectedLanguages.clear()

            refreshItems()
        }

        fun isAllSelected(): Boolean = languages.all {
            if (it.code == "und") {
                true
            } else {
                selectedLanguages.contains(it.id)
            }
        }
    }
}
