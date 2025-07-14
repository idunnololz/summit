package com.idunnololz.summit.lemmy.modlogs.filter

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.ModlogActionType
import com.idunnololz.summit.databinding.DialogFragmentModLogsFilterBinding
import com.idunnololz.summit.lemmy.getName
import com.idunnololz.summit.lemmy.modlogs.ModLogsFilterConfig
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.setupToolbar
import kotlinx.parcelize.Parcelize

class ModLogsFilterDialogFragment : BaseDialogFragment<DialogFragmentModLogsFilterBinding>() {

  companion object {
    const val REQUEST_KEY = "ModLogsFilterDialogFragment_REQUEST_KEY"
    const val REQUEST_KEY_RESULT = "result"

    private const val USER_REQUEST_KEY = "USER_REQUEST_KEY"
    private const val MOD_REQUEST_KEY = "MOD_REQUEST_KEY"

    fun show(fragmentManager: FragmentManager, filter: ModLogsFilterConfig) {
      ModLogsFilterDialogFragment()
        .apply {
          arguments = ModLogsFilterDialogFragmentArgs(filter).toBundle()
        }
        .show(fragmentManager, "ModLogsFilterDialogFragment")
    }
  }

  @Parcelize
  class Result(
    val filterConfig: ModLogsFilterConfig,
  ) : Parcelable

  data class ActionTypeOption(
    val type: ModlogActionType,
    val name: String,
  ) {
    override fun toString(): String = name
  }

  private val args: ModLogsFilterDialogFragmentArgs by navArgs()
  private val viewModel: ModLogsFilterViewModel by viewModels()

  override fun onStart() {
    super.onStart()
    setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentModLogsFilterBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    if (savedInstanceState == null) {
      viewModel.filter = args.filter
    }

    fun onResult(k: String, bundle: Bundle) {
      val result = bundle.getParcelableCompat<PersonPickerDialogFragment.Result>(
        PersonPickerDialogFragment.REQUEST_KEY_RESULT,
      )

      if (result != null) {
        if (k == USER_REQUEST_KEY) {
          viewModel.filter = viewModel.filter.copy(filterByPerson = result.personRef)
          binding.userFilterEditText.setText(result.personRef.fullName)
        } else {
          viewModel.filter = viewModel.filter.copy(filterByMod = result.personRef)
          binding.modFilterEditText.setText(result.personRef.fullName)
        }
      }
    }

    childFragmentManager.setFragmentResultListener(
      USER_REQUEST_KEY,
      viewLifecycleOwner,
    ) { k, bundle ->
      onResult(k, bundle)
    }
    childFragmentManager.setFragmentResultListener(
      MOD_REQUEST_KEY,
      viewLifecycleOwner,
    ) { k, bundle ->
      onResult(k, bundle)
    }

    with(binding) {
      setupToolbar(toolbar, getString(R.string.filter_mod_logs))

      val options = ModlogActionType.entries.map {
        ActionTypeOption(
          it,
          it.getName(context),
        )
      }

      actionFilterText.setAdapter(
        ArrayAdapter(
          context,
          R.layout.auto_complete_simple_item,
          options,
        ),
      )
      actionFilterText.setText(viewModel.filter.filterByActionType.getName(context), false)
      actionFilterText.setOnItemClickListener { _, _, position, _ ->
        viewModel.filter = viewModel.filter.copy(filterByActionType = options[position].type)
      }

      userFilterEditText.setText(viewModel.filter.filterByPerson?.fullName ?: "")
      userFilterEditText.setOnClickListener {
        PersonPickerDialogFragment.show(
          childFragmentManager,
          userFilterEditText.text?.toString(),
          USER_REQUEST_KEY,
        )
      }
      userFilter.setEndIconOnClickListener {
        viewModel.filter = viewModel.filter.copy(filterByPerson = null)
        userFilterEditText.setText("")
      }

      modFilterEditText.setText(viewModel.filter.filterByMod?.fullName ?: "")
      modFilterEditText.setOnClickListener {
        PersonPickerDialogFragment.show(
          childFragmentManager,
          modFilterEditText.text?.toString(),
          MOD_REQUEST_KEY,
        )
      }
      modFilter.setEndIconOnClickListener {
        viewModel.filter = viewModel.filter.copy(filterByMod = null)
        modFilterEditText.setText("")
      }

      filter.setOnClickListener {
        setFragmentResult(
          REQUEST_KEY,
          bundleOf(
            REQUEST_KEY_RESULT to Result(
              filterConfig = viewModel.filter,
            ),
          ),
        )
        dismiss()
      }
    }
  }
}
