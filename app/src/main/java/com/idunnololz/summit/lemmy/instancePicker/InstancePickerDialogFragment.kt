package com.idunnololz.summit.lemmy.instancePicker

import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentInstancePickerBinding
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class InstancePickerDialogFragment :
    BaseDialogFragment<DialogFragmentInstancePickerBinding>(),
    FullscreenDialogFragment,
    BackPressHandler {

    companion object {
        const val REQUEST_KEY = "InstancePickerDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager) {
            InstancePickerDialogFragment()
                .showAllowingStateLoss(fragmentManager, "InstancePickerDialogFragment")
        }
    }

    private var adapter: InstanceAdapter? = null

    private val viewModel: InstancePickerViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Parcelize
    data class Result(
        val instance: String,
    ) : Parcelable

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

        setBinding(DialogFragmentInstancePickerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        val context = requireContext()

        with(binding) {
            searchEditText.addTextChangedListener {
                val query = it?.toString() ?: ""
                adapter?.setQuery(query) {
                    resultsRecyclerView.scrollToPosition(0)
                }
                viewModel.doQuery(query)
            }
            searchBar.hint = viewModel.instance
            searchEditText.hint = viewModel.instance

            searchEditText.setOnKeyListener(
                object : View.OnKeyListener {
                    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                        if (event?.action == KeyEvent.ACTION_DOWN &&
                            keyCode == KeyEvent.KEYCODE_ENTER
                        ) {
                            setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(
                                    REQUEST_KEY_RESULT to Result(searchEditText.text.toString()),
                                ),
                            )
                            dismiss()
                            return true
                        }
                        return false
                    }
                },
            )

            adapter = InstanceAdapter(
                context = context,
                offlineManager = offlineManager,
                canSelectMultipleCommunities = false,
                onSingleInstanceSelected = { instance ->
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            REQUEST_KEY_RESULT to Result(instance),
                        ),
                    )
                    dismiss()
                },
            )
            resultsRecyclerView.adapter = adapter
            resultsRecyclerView.setHasFixedSize(true)
            resultsRecyclerView.layoutManager = LinearLayoutManager(context)

            viewModel.searchResults.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        adapter?.setQueryServerResults(InstancePickerViewModel.SearchResults())
                    }
                    is StatefulData.Loading -> {
                        adapter?.setQueryServerResultsInProgress()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        adapter?.setQueryServerResults(it.data)
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (!binding.searchEditText.text.isNullOrBlank()) {
            binding.searchEditText.setText("")
            return true
        }

        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }
}
