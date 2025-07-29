package com.idunnololz.summit.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentAddOrEditTemplateBinding
import com.idunnololz.summit.databinding.DialogFragmentReasonBinding
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddOrEditTemplateFragmentDialog :
  BaseDialogFragment<DialogFragmentAddOrEditTemplateBinding>(),
  FullscreenDialogFragment {

    companion object {
      fun show(template: TemplateEntry?, fragmentManager: FragmentManager) {
        AddOrEditTemplateFragmentDialog()
          .apply {
            arguments = AddOrEditTemplateFragmentDialogArgs(template).toBundle()
          }
          .show(fragmentManager, "AddOrEditTemplateFragmentDialog")
      }
    }

  private val args: AddOrEditTemplateFragmentDialogArgs by navArgs()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentAddOrEditTemplateBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      val template = args.template

      setupToolbar(
        toolbar = toolbar,
        title = if (template != null) {
          getString(R.string.edit_template)
        } else {
          getString(R.string.add_template)
        },
      )
    }
  }
}