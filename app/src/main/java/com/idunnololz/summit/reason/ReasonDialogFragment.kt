package com.idunnololz.summit.reason

import android.os.Bundle
import android.os.Parcelable
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentReasonBinding
import com.idunnololz.summit.templates.AddOrEditTemplateFragmentDialog
import com.idunnololz.summit.templates.TemplatesAdapter
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ReasonDialogFragment :
  BaseDialogFragment<DialogFragmentReasonBinding>(),
    FullscreenDialogFragment {

  companion object {
    fun show(fragmentManager: FragmentManager, title: String, positiveButton: String) =
      ReasonDialogFragment()
        .apply {
          arguments = ReasonDialogFragmentArgs(
            title = title,
            positiveButton = positiveButton,
          ).toBundle()
        }
        .show(fragmentManager, "ReasonDialogFragment")

    const val REQUEST_KEY = "ReasonDialogFragment_req_key"
    const val RESULT_KEY = "result"
  }

  private val args: ReasonDialogFragmentArgs by navArgs()

  private val viewModel: ReasonViewModel by viewModels()

  @Parcelize
  data class Result(
    val isOk: Boolean,
    val reason: String?,
  ) : Parcelable

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentReasonBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    with(binding) {
      requireMainActivity().apply {
        insetViewAutomaticallyByPadding(viewLifecycleOwner, root)
      }

      setupToolbar(toolbar, args.title)

      toolbar.setNavigationOnClickListener {
        setFragmentResult(
          REQUEST_KEY,
          Bundle().apply {
            putParcelable(
              RESULT_KEY,
              Result(
                isOk = false,
                reason = null,
              ),
            )
          },
        )
        dismiss()
      }

      toolbar.addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(
          menu: Menu,
          menuInflater: MenuInflater,
        ) {
          menuInflater.inflate(R.menu.menu_reason, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            R.id.send -> {
              setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                  putParcelable(
                    RESULT_KEY,
                    Result(
                      isOk = true,
                      reason = reasonEditText.text.toString(),
                    ),
                  )
                },
              )
              dismiss()
            }
          }

          return true
        }

      })

      val adapter = TemplatesAdapter(
        onTemplateClick = {
          it.data?.content?.let {
            reasonEditText.setText(it)
          }
        },
        onAddTemplateClick = {
          AddOrEditTemplateFragmentDialog.show(
            template = null,
            fragmentManager = parentFragmentManager
          )
        },
        onEditTemplateClick = {
          AddOrEditTemplateFragmentDialog.show(
            template = it,
            fragmentManager = parentFragmentManager
          )
        },
      )

      recyclerView.setHasFixedSize(false)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.adapter = adapter

      viewModel.templates.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            templatesLoadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading -> {
            templatesLoadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            templatesLoadingView.hideAll()

            adapter.setData(it.data)
          }
        }
      }
    }
  }
}