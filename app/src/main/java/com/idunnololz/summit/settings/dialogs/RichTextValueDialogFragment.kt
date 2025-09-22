package com.idunnololz.summit.settings.dialogs

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentRichTextValueBinding
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RichTextValueDialogFragment : BaseDialogFragment<DialogFragmentRichTextValueBinding>() {

  companion object {
    private const val ARG_TITLE = "ARG_TITLE"
    private const val ARG_KEY_ID = "ARG_KEY_ID"
    private const val ARG_CURRENT_VALUE = "ARG_CURRENT_VALUE"
    private const val ARG_RESET_VALUE = "ARG_RESET_VALUE"
    private const val ARG_SHOW_RESET_BUTTON = "ARG_SHOW_RESET_BUTTON"
    private const val ARG_SUPPORTS_RICH_TEXT = "ARG_SUPPORTS_RICH_TEXT"

    fun newInstance(
      title: String,
      key: Int,
      currentValue: String?,
      showResetButton: Boolean,
      resetValue: String?,
      supportsRichText: Boolean,
    ) =
      RichTextValueDialogFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_TITLE, title)
          putInt(ARG_KEY_ID, key)
          putString(ARG_CURRENT_VALUE, currentValue)
          putBoolean(ARG_SHOW_RESET_BUTTON, showResetButton)
          putString(ARG_RESET_VALUE, resetValue)
          putBoolean(ARG_SUPPORTS_RICH_TEXT, supportsRichText)
        }
      }
  }

  private val viewModel: RichTextValueViewModel by viewModels()

  @Inject
  lateinit var textFieldToolbarManager: TextFieldToolbarManager

  private val supportsRichText
    get() = requireArguments().getBoolean(ARG_SUPPORTS_RICH_TEXT)

  private val launcher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == Activity.RESULT_OK) {
        val uri = it.data?.data

        if (uri != null) {
          viewModel.uploadImage(uri)
        }
      }
    }

  @Inject
  lateinit var preferences: Preferences

  override fun onStart() {
    super.onStart()

    val dialog = dialog
    if (dialog != null && supportsRichText) {
      val width = ViewGroup.LayoutParams.MATCH_PARENT
      val height = ViewGroup.LayoutParams.MATCH_PARENT

      val window = checkNotNull(dialog.window)
      window.setLayout(width, height)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentRichTextValueBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      val args = requireArguments()

      val showResetButton = args.getBoolean(ARG_SHOW_RESET_BUTTON)
      val resetValue = args.getString(ARG_RESET_VALUE)
      val supportsRichText = args.getBoolean(ARG_SUPPORTS_RICH_TEXT)

      textEditor.hint = args.getString(ARG_TITLE)
      textEditor.setText(args.getString(ARG_CURRENT_VALUE))

      if (supportsRichText) {
        textEditor.updateLayoutParams {
          height = 0
        }

        val textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
          context,
          postBodyToolbar,
        )

        textFormatToolbar.setupTextFormatterToolbar(
          editText = textEditor,
          referenceTextView = null,
          lifecycleOwner = viewLifecycleOwner,
          fragmentManager = childFragmentManager,
          onChooseImageClick = {
            val bottomMenu = BottomMenu(context).apply {
              setTitle(R.string.insert_image)
              addItemWithIcon(
                R.id.from_camera,
                R.string.take_a_photo,
                R.drawable.baseline_photo_camera_24,
              )
              addItemWithIcon(
                R.id.from_gallery,
                R.string.choose_from_gallery,
                R.drawable.baseline_image_24,
              )
              addItemWithIcon(
                R.id.from_camera_with_editor,
                R.string.take_a_photo_with_editor,
                R.drawable.baseline_photo_camera_24,
              )
              addItemWithIcon(
                R.id.from_gallery_with_editor,
                R.string.choose_from_gallery_with_editor,
                R.drawable.baseline_image_24,
              )

              setOnMenuItemClickListener {
                when (it.id) {
                  R.id.from_camera -> {
                    val intent = ImagePicker.with(requireActivity())
                      .cameraOnly()
                      .createIntent()
                    launcher.launch(intent)
                  }

                  R.id.from_gallery -> {
                    val intent = ImagePicker.with(requireActivity())
                      .galleryOnly()
                      .createIntent()
                    launcher.launch(intent)
                  }

                  R.id.from_camera_with_editor -> {
                    val intent = ImagePicker.with(requireActivity())
                      .cameraOnly()
                      .crop()
                      .cropFreeStyle()
                      .createIntent()
                    launcher.launch(intent)
                  }

                  R.id.from_gallery_with_editor -> {
                    val intent = ImagePicker.with(requireActivity())
                      .galleryOnly()
                      .crop()
                      .cropFreeStyle()
                      .createIntent()
                    launcher.launch(intent)
                  }
                }
              }
            }

            bottomMenu.show(
              bottomMenuContainer = requireMainActivity(),
              bottomSheetContainer = binding.root,
              expandFully = true,
              handleBackPress = true,
              handleInsets = false,
              onBackPressedDispatcher = onBackPressedDispatcher,
            )
          },
          onPreviewClick = {
            PreviewCommentDialogFragment()
              .apply {
                arguments = PreviewCommentDialogFragmentArgs(
                  viewModel.instance,
                  textEditor.text.toString(),
                ).toBundle()
              }
              .showAllowingStateLoss(childFragmentManager, "AA")
          },
          onAddLinkClick = {
            AddLinkDialogFragment.show(
              binding.textEditor.getSelectedText(),
              childFragmentManager,
            )
          },
        )

        viewModel.uploadImageEvent.observe(viewLifecycleOwner) {
          when (it) {
            is StatefulData.Error -> {
              loadingView.hideAll()
              OldAlertDialogFragment.Builder()
                .setMessage(
                  getString(
                    R.string.error_unable_to_send_post,
                    it.error::class.qualifiedName,
                    it.error.message,
                  ),
                )
                .createAndShow(childFragmentManager, "ASDS")
            }
            is StatefulData.Loading -> {
              loadingView.showProgressBar()
            }
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
              loadingView.hideAll()
              viewModel.uploadImageEvent.clear()

              textFormatToolbar.onImageUploaded(it.data.url)
            }
          }
        }
      } else {
        textEditor.updateLayoutParams {
          height = WRAP_CONTENT
        }
        postBodyToolbar.visibility = View.GONE
      }

      if (showResetButton) {
        resetButton.setOnClickListener {
          dismiss()
          (parentFragment as SettingValueUpdateCallback).updateValue(
            key = args.getInt(ARG_KEY_ID),
            value = resetValue,
          )
        }
        resetButton.visibility = View.VISIBLE
      } else {
        resetButton.visibility = View.GONE
      }

      positiveButton.setOnClickListener {
        dismiss()
        (parentFragment as SettingValueUpdateCallback).updateValue(
          key = args.getInt(ARG_KEY_ID),
          value = textEditor.text?.toString(),
        )
      }
      negativeButton.setOnClickListener {
        dismiss()
      }
    }
  }
}
