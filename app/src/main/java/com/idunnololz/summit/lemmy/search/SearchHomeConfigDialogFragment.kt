package com.idunnololz.summit.lemmy.search

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSearchHomeConfigBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.SearchHomeConfig
import com.idunnololz.summit.settings.SearchHomeSettings
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchHomeConfigDialogFragment : BaseDialogFragment<FragmentSearchHomeConfigBinding>() {

    companion object {
        const val REQUEST_KEY = "SearchHomeConfigDialogFragment_req"

        fun show(fragmentManager: FragmentManager) {
            SearchHomeConfigDialogFragment()
                .show(fragmentManager, "SearchHomeConfigFragment")
        }
    }

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: SearchHomeSettings

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

        setBinding(FragmentSearchHomeConfigBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            binding.toolbar.title = getString(R.string.search_screen_settings)
            binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            binding.toolbar.setNavigationOnClickListener {
                dismiss()
            }
            binding.toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            updateUi()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        setFragmentResult(
            requestKey = REQUEST_KEY,
            result = bundleOf(),
        )
    }

    private fun updateUi() {
        var searchHomeConfig = preferences.searchHomeConfig

        fun updateConfig(config: SearchHomeConfig) {
            preferences.searchHomeConfig = config
            searchHomeConfig = config
        }

        with(binding) {
            settings.searchSuggestions.bindTo(
                searchSuggestions,
                { searchHomeConfig.showSearchSuggestions },
                {
                    updateConfig(searchHomeConfig.copy(showSearchSuggestions = it))
                },
            )
            settings.subscribedCommunities.bindTo(
                yourCommunities,
                { searchHomeConfig.showSubscribedCommunities },
                {
                    updateConfig(searchHomeConfig.copy(showSubscribedCommunities = it))
                },
            )
            settings.topCommunities.bindTo(
                topCommunities,
                { searchHomeConfig.showTopCommunity7DaysSuggestions },
                {
                    updateConfig(searchHomeConfig.copy(showTopCommunity7DaysSuggestions = it))
                },
            )
            settings.trendingCommunities.bindTo(
                trendingCommunities,
                { searchHomeConfig.showTrendingCommunitySuggestions },
                {
                    updateConfig(searchHomeConfig.copy(showTrendingCommunitySuggestions = it))
                },
            )
            settings.risingCommunities.bindTo(
                risingCommunities,
                { searchHomeConfig.showRisingCommunitySuggestions },
                {
                    updateConfig(searchHomeConfig.copy(showRisingCommunitySuggestions = it))
                },
            )
        }
    }
}
