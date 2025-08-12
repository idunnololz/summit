package com.idunnololz.summit.settings.postsFeed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentAddFilterBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.filterLists.FilterEntryOptions
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import java.util.regex.Pattern

class AddOrEditFilterDialogFragment : BaseDialogFragment<DialogFragmentAddFilterBinding>() {
  private val args by navArgs<AddOrEditFilterDialogFragmentArgs>()

  companion object {
    const val REQUEST_KEY = "AddFilterDialogFragment_req_key"
    const val REQUEST_KEY_RESULT = "result"

    private const val SIS_CURRENT_FILTER = "SIS_CURRENT_FILTER"

    fun newInstance(initialEntry: FilterEntry): AddOrEditFilterDialogFragment =
      AddOrEditFilterDialogFragment().apply {
        arguments = AddOrEditFilterDialogFragmentArgs(initialEntry).toBundle()
      }
  }

  private var currentFilter: FilterEntry? = null

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

    setBinding(DialogFragmentAddFilterBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      title.text = getString(R.string.add_filter)

      currentFilter = currentFilter
        ?: savedInstanceState?.getParcelableCompat(SIS_CURRENT_FILTER)
        ?: args.filterToEdit

      filterField.editText?.setText(currentFilter?.filter)

      regexSwitch.setOnCheckedChangeListener { view, isChecked ->
        if (isChecked) {
          matchWholeWordSwitch.isEnabled = false
        } else {
          matchWholeWordSwitch.isEnabled = true
        }
      }
      regexSwitch.isChecked = currentFilter?.isRegex == true
      matchWholeWordSwitch.isChecked = currentFilter?.options?.matchWholeWord == true

      cancel.setOnClickListener {
        dismiss()
      }
      add.setOnClickListener {
        commitAndDismiss()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    outState.putParcelable(SIS_CURRENT_FILTER, currentFilter)
  }

  private fun commitAndDismiss() {
    if (!isBindingAvailable()) return

    val filterText = binding.filterField.editText?.text?.toString()
    val isRegex = binding.regexSwitch.isChecked
    val matchWholeWord = if (isRegex) {
      null
    } else {
      binding.matchWholeWordSwitch.isChecked
    }

    if (filterText.isNullOrBlank()) {
      binding.filterField.error = getString(R.string.error_filter_cannot_be_blank)
      return
    }
    if (isRegex) {
      try {
        Pattern.compile(filterText)
      } catch (e: Exception) {
        ErrorDialogFragment.show(getString(R.string.error_invalid_regex), e, childFragmentManager)
        return
      }
    }

    val filter = args.filterToEdit.copy(
      filter = filterText,
      isRegex = isRegex,
      options = (args.filterToEdit.options ?: FilterEntryOptions()).copy(
        matchWholeWord = matchWholeWord,
      ),
    )
    setFragmentResult(
      REQUEST_KEY,
      bundleOf(REQUEST_KEY_RESULT to filter),
    )

    dismiss()
  }
}
