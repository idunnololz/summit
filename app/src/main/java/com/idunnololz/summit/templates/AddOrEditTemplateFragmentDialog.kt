package com.idunnololz.summit.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.databinding.DialogFragmentAddOrEditTemplateBinding
import com.idunnololz.summit.databinding.DialogFragmentReasonBinding
import com.idunnololz.summit.reason.ReasonDialogFragment.Companion.REQUEST_KEY
import com.idunnololz.summit.reason.ReasonDialogFragment.Companion.RESULT_KEY
import com.idunnololz.summit.reason.ReasonDialogFragment.Result
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
  private val viewModel: AddOrEditTemplateViewModel by viewModels()

  private var onViewCreated: Boolean = false

  @Inject
  lateinit var templatesManager: TemplatesManager
  @Inject
  lateinit var accountManager: AccountManager

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
      requireMainActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, root)
      }

      val template = args.template

      setupToolbar(
        toolbar = toolbar,
        title = if (template != null) {
          getString(R.string.edit_template)
        } else {
          getString(R.string.add_template)
        },
      )
      toolbar.addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(
          menu: Menu,
          menuInflater: MenuInflater,
        ) {
          menuInflater.inflate(R.menu.menu_template, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            R.id.save -> {
              val account = accountManager.currentAccount.value
              val data = TemplateData.RegistrationApplicationRejectionTemplateData(
                content = contentEditText.text.toString(),
                accountId = account.id,
                title = titleEditText.text.toString(),
                accountInstance = account.instance,
              )

              if (template != null) {
                templatesManager.updateTemplateAsync(
                  entryId = template.id,
                  templateData = data,
                  showToast = true,
                )
              } else {
                templatesManager.saveTemplateAsync(
                  templateData = data,
                  showToast = true
                )
              }
              dismiss()
            }
            R.id.delete -> {
              templatesManager.deleteTemplateWithIdAsync(template?.id)
              dismiss()
            }
          }

          return true
        }
      })

      if (savedInstanceState == null && !onViewCreated) {
        template?.data?.let {
          titleEditText.setText(it.title)
          contentEditText.setText(it.content)
        }
      }

      onViewCreated = true
    }
  }
}