package com.idunnololz.summit.settings

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.appbar.AppBarLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BasicSettingItemBinding
import com.idunnololz.summit.databinding.FragmentSettingsBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    @Inject
    lateinit var settingsManager: SettingsManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()

            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.collapsingToolbarLayout)

            setSupportActionBar(binding.searchBar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.settings)
            supportActionBar?.hide()

            binding.searchBar.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags = 0
            }
        }

        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = SettingItemsAdapter(settingsManager.getSettingsForMainPage())
        }
    }

    private inner class SettingItemsAdapter(
        private val data: List<BasicSettingItem>
    ) : Adapter<ViewHolder>() {

        private val adapterHelper = AdapterHelper<SettingItem>(
            areItemsTheSame = { old, new ->
                old.id == new.id
            }
        ).apply {
            addItemType(BasicSettingItem::class, BasicSettingItemBinding::inflate) { item, b, h ->
                b.icon.setImageResource(item.icon)
                b.title.text = item.title
                b.desc.text = item.description

                b.root.setOnClickListener {

                    when (item.id) {
                        R.id.setting_view_type -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingViewTypeFragment()
                            findNavController().navigateSafe(directions)
                        }
                        R.id.setting_theme -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingThemeFragment()
                            findNavController().navigateSafe(directions)
                        }
                    }
                }
            }
        }

        init {
            refreshItems()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            adapterHelper.setItems(data, this)
        }

    }
}