package com.idunnololz.summit.lemmy.personPicker

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentPersonPickerBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class PersonPickerDialogFragment :
  BaseDialogFragment<DialogFragmentPersonPickerBinding>(),
  FullscreenDialogFragment {

  companion object {
    const val REQUEST_KEY = "PersonPickerDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    fun show(fragmentManager: FragmentManager, prefill: String? = null) {
      PersonPickerDialogFragment()
        .apply {
          arguments = PersonPickerDialogFragmentArgs(
            prefill,
          ).toBundle()
        }
        .showAllowingStateLoss(fragmentManager, "PersonPickerDialogFragment")
    }
  }

  private val args: PersonPickerDialogFragmentArgs by navArgs()

  private var adapter: PersonAdapter? = null

  private val viewModel: PersonPickerViewModel by viewModels()

  private var isPrefillUsed: Boolean = false

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  private val searchEditTextBackPressedHandler = object : OnBackPressedCallback(false) {
    override fun handleOnBackPressed() {
      binding.searchEditText.setText("")
    }
  }

  @Parcelize
  data class Result(
    val personRef: PersonRef.PersonRefByName,
    val icon: String?,
    val personId: Long,
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

    setBinding(DialogFragmentPersonPickerBinding.inflate(inflater, container, false))

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

        searchEditTextBackPressedHandler.isEnabled = query.isNotEmpty()
      }

      adapter = PersonAdapter(
        context = context,
        offlineManager = offlineManager,
        canSelectMultiplePersons = false,
        instance = viewModel.instance,
        onSinglePersonSelected = { ref, icon, personId ->
          setFragmentResult(
            REQUEST_KEY,
            bundleOf(
              REQUEST_KEY_RESULT to Result(
                personRef = ref,
                icon = icon,
                personId = personId,
              ),
            ),
          )
          dismiss()
        },
      )
      resultsRecyclerView.setup(animationsHelper)
      resultsRecyclerView.adapter = adapter
      resultsRecyclerView.setHasFixedSize(true)
      resultsRecyclerView.layoutManager = LinearLayoutManager(context)

      viewModel.searchResults.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            adapter?.setQueryServerResults(null)
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

      val prefill = args.prefill

      if (prefill != null &&
        !isPrefillUsed &&
        searchEditText.text.isNullOrBlank() &&
        savedInstanceState == null
      ) {
        isPrefillUsed = true
        searchEditText.setText(prefill)
      }
    }

    onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      searchEditTextBackPressedHandler,
    )
  }
}
