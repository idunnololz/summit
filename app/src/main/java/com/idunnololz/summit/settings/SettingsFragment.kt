package com.idunnololz.summit.settings

import android.os.Bundle
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
import com.idunnololz.summit.databinding.SubgroupSettingItemBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var mainSettings: MainSettings


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
            recyclerView.adapter = SettingItemsAdapter(
                onSettingClick = {
                    when (it) {
                        mainSettings.settingViewType.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingViewTypeFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        mainSettings.settingTheme.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingThemeFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        mainSettings.settingPostAndComment.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingPostAndCommentsFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        mainSettings.settingLemmyWeb.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingWebFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        mainSettings.settingHistory.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingHistoryFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        mainSettings.settingCache.id -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingCacheFragment()
                            findNavController().navigateSafe(directions)
                            true
                        }
                        else -> false
                    }
                },
                childFragmentManager,
            ).apply {
                this.data = settingsManager.getSettingsForMainPage()
            }
        }
    }

}