package com.idunnololz.summit.drafts.addOrEditTemplate

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentAddOrEditPostTemplateBinding
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarHelper
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.lemmy.UploadImageViewModel
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByMargins
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class AddOrEditPostTemplateFragment  :
  BaseDialogFragment<FragmentAddOrEditPostTemplateBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun show(fragmentManager: FragmentManager, instance: String) {
      AddOrEditPostTemplateFragment().apply {
        arguments = AddOrEditPostTemplateFragmentArgs(instance).toBundle()
      }.showAllowingStateLoss(fragmentManager, "PostsAndCommentsTemplatesFragment")
    }
  }

  private val args by navArgs<AddOrEditPostTemplateFragmentArgs>()

  private val uploadImageViewModel: UploadImageViewModel by viewModels()

  @Inject
  lateinit var textFieldToolbarManager: TextFieldToolbarManager

  private var textFormatToolbar: TextFormatToolbarViewHolder? = null
  private var textFieldToolbarHelper: TextFieldToolbarHelper? = null

  private val launcher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == Activity.RESULT_OK) {
        val uri = it.data?.data

        if (uri != null) {
          uploadImageViewModel.uploadImage(uri)
        }
      }
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentAddOrEditPostTemplateBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireMainActivity().apply {
        insetViewAutomaticallyByMargins(viewLifecycleOwner, contentOuter)

        setupToolbar(
          toolbar,
          getString(R.string.new_template)
        )
      }

      textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
        postBodyToolbar.removeAllViews()

        textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
          context,
          binding.postBodyToolbar,
        )

        textFormatToolbar?.setupTextFormatterToolbar(
          editText = bodyEditText,
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
              addItemWithIcon(
                R.id.use_a_saved_image,
                R.string.use_a_saved_image,
                R.drawable.baseline_save_24,
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
                  R.id.use_a_saved_image -> {
                    ChooseSavedImageDialogFragment()
                      .apply {
                        arguments = ChooseSavedImageDialogFragmentArgs().toBundle()
                      }
                      .showAllowingStateLoss(
                        childFragmentManager,
                        "ChooseSavedImageDialogFragment",
                      )
                  }
                }
              }
            }

            bottomMenu.show(
              bottomMenuContainer = requireMainActivity(),
              bottomSheetContainer = binding.root,
              expandFully = true,
              handleBackPress = false,
            )
          },
          onAddLinkClick = {
            AddLinkDialogFragment.show(
              bodyEditText.getSelectedText(),
              childFragmentManager,
            )
          },
          onPreviewClick = {
            val postStr = buildString {
              appendLine("## ${binding.titleEditText.text}")
              appendLine()
              appendLine(bodyEditText.text.toString())
            }
            PreviewCommentDialogFragment()
              .apply {
                arguments = PreviewCommentDialogFragmentArgs(
                  args.instance,
                  postStr,
                ).toBundle()
              }
              .showAllowingStateLoss(childFragmentManager, "AA")
          },
          onDraftsClick = {
            DraftsDialogFragment.show(childFragmentManager, DraftTypes.Post)
          },
          onSettingsClick = {
            EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
          },
        )
      }


      textFieldToolbarHelper = TextFieldToolbarHelper(
        root = root,
        postBodyToolbar = postBodyToolbarContainer,
        postBodyToolbarPlaceholder = postBodyToolbarPlaceholder,
        postBodyToolbarPlaceholder2 = postBodyToolbarPlaceholder2,
        bodyEditText = bodyEditText,
        postTextDivider = null,
        scrollView = scrollView,
        getInsetsProvider = { getMainActivity() },
        editTextsThatUseToolbar = listOf(
          bodyEditText,
          titleEditText,
        ),
        lifecycleOwner = viewLifecycleOwner,
        onPositionChange = { isSticky ->
          if (isSticky) {
            postBodyToolbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              marginStart = 0
              marginEnd = 0
            }
          } else {
            postBodyToolbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              marginStart = context.resources.getDimensionPixelOffset(R.dimen.padding)
              marginEnd = context.resources.getDimensionPixelOffset(R.dimen.padding)
            }
          }
        },
      )
      textFieldToolbarHelper?.registerListeners()
    }

  }
}