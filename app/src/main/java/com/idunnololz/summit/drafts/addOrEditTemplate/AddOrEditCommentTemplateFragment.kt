package com.idunnololz.summit.drafts.addOrEditTemplate

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.github.drjacky.imagepicker.ImagePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.StateListSizeChange
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentAddOrEditCommentTemplateBinding
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
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByMargins
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class AddOrEditCommentTemplateFragment :
  BaseDialogFragment<FragmentAddOrEditCommentTemplateBinding>(),
  FullscreenDialogFragment {

  companion object {
    fun show(
      fragmentManager: FragmentManager,
      instance: String,
      templateToEditId: Long? = null,
    ) {
      AddOrEditCommentTemplateFragment().apply {
        arguments = AddOrEditCommentTemplateFragmentArgs(
          instance = instance,
          templateToEdit = TemplateToEdit(
            templateToEditId
          ),
        ).toBundle()
      }.showAllowingStateLoss(fragmentManager, "AddOrEditCommentTemplateFragment")
    }
  }

  private val args by navArgs<AddOrEditCommentTemplateFragmentArgs>()

  private val viewModel: AddOrEditCommentTemplateViewModel by viewModels()
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

    setBinding(FragmentAddOrEditCommentTemplateBinding.inflate(inflater, container, false))

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
          if (args.templateToEdit.entryId != null) {
            getString(R.string.edit_template)
          } else {
            getString(R.string.new_template)
          },
        )
      }

      viewModel.loadTemplateIfNeeded(args.templateToEdit)

      registerTemplateIdListener()

      textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
        commentBodyToolbar.removeAllViews()

        textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
          context,
          binding.commentBodyToolbar,
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
            val commentStr = buildString {
              appendLine(bodyEditText.text.toString())
            }
            PreviewCommentDialogFragment()
              .apply {
                arguments = PreviewCommentDialogFragmentArgs(
                  args.instance,
                  commentStr,
                ).toBundle()
              }
              .showAllowingStateLoss(childFragmentManager, "AA")
          },
          onDraftsClick = {
            DraftsDialogFragment.show(childFragmentManager, DraftTypes.Comment)
          },
          onSettingsClick = {
            EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
          },
        )
      }

      textFieldToolbarHelper = TextFieldToolbarHelper(
        root = root,
        textBodyToolbar = commentBodyToolbarContainer,
        textBodyToolbarPlaceholder = commentBodyToolbarPlaceholder,
        textBodyToolbarPlaceholder2 = commentBodyToolbarPlaceholder2,
        bodyEditText = bodyEditText,
        textDivider = null,
        scrollView = scrollView,
        getInsetsProvider = { getMainActivity() },
        editTextsThatUseToolbar = listOf(
          bodyEditText,
        ),
        lifecycleOwner = viewLifecycleOwner,
        toolbarTopMargin = context.resources.getDimensionPixelOffset(R.dimen.padding_quarter),
        onPositionChange = { isSticky ->
          if (isSticky) {
            commentBodyToolbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              marginStart = 0
              marginEnd = 0
            }
            commentBodyToolbarContainer.radius = 0f
          } else {
            commentBodyToolbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
              marginStart = context.resources.getDimensionPixelOffset(R.dimen.padding)
              marginEnd = context.resources.getDimensionPixelOffset(R.dimen.padding)
            }
            commentBodyToolbarContainer.radius =
              context.resources.getDimensionPixelOffset(R.dimen.padding_half).toFloat()
          }
        },
      )
      textFieldToolbarHelper?.registerListeners()

      viewModel.templateToEditData.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error<*> -> {
            loadingView.showDefaultErrorMessageFor(it.error)
          }
          is StatefulData.Loading<*> -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted<*> -> {
            loadingView.hideAll()
          }
          is StatefulData.Success -> {
            loadingView.hideAll()

            templateNameEditText.setText(it.data.name)
            bodyEditText.setText(it.data.content)
          }
        }
      }
    }
  }

  fun registerTemplateIdListener() {
    if (!isBindingAvailable()) return

    with(binding) {
      viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
          viewModel.templateId.collect { templateId ->
            val transition = TransitionSet()
              .addTransition(Fade())
              .addTransition(ChangeBounds())
              .setOrdering(TransitionSet.ORDERING_TOGETHER)
              .setDuration(200)

            TransitionManager.beginDelayedTransition(buttonGroup, transition)

            updateButtonGroup(templateId = templateId)

            buttonGroup.requestLayout()
          }
        }
      }
    }
  }

  /**
   * We set the buttons up programmatically because [ButtonGroup] is finicky and buggy when
   * buttons are show/hid. By populating the [ButtonGroup] via code, we can dodge most of these
   * bugs.
   */
  private fun updateButtonGroup(
    templateId: Long?
  ) {
    if (!isBindingAvailable()) return

    with(binding) {
      buttonGroup.removeAllViews()

      if (templateId != null) {
        val deleteButton = newButtonGroupButton(
          context = buttonGroup.context,
          id = R.id.delete_template_button
        ).apply {
          icon = AppCompatResources.getDrawable(context, R.drawable.outline_delete_24)
          iconPadding = 0
          backgroundTintList = ColorStateList.valueOf(
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorErrorContainer),
          )
          iconTint = ColorStateList.valueOf(
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnErrorContainer),
          )
        }
        deleteButton.setOnClickListener {
          viewModel.deleteTemplate(templateId)
          dismiss()
        }
        buttonGroup.addView(deleteButton)
      }

      val saveButton = newButtonGroupButton(
        context = buttonGroup.context,
        id = R.id.save_template_button
      ).apply {
        text = context.getString(R.string.save_template)
        icon = AppCompatResources.getDrawable(context, R.drawable.outline_save_24)
      }

      saveButton.setOnClickListener {
        viewLifecycleOwner.lifecycleScope.launch {
          viewModel.save(
            name = templateNameEditText.text.toString(),
            body = bodyEditText.text.toString(),
          )

          dismiss()
        }
      }
      buttonGroup.addView(saveButton)
      buttonGroup.enableDefaultButtonSizeChange()
    }
  }

  private fun newButtonGroupButton(context: Context, id: Int): MaterialButton {
    val themedContext = ContextThemeWrapper(
      context,
      com.google.android.material.R.style.Widget_Material3Expressive_Button
    )
    return MaterialButton(themedContext).apply {
      this.id = id
      layoutParams = MaterialButtonGroup.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        2f,
      )
      gravity = Gravity.CENTER
      iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
      iconSize = Utils.convertDpToPixel(24f).toInt()
    }
  }

  private fun MaterialButtonGroup.enableDefaultButtonSizeChange() {
    doOnNextLayout {
      val attributes = context.obtainStyledAttributes(
        com.google.android.material.R.style.Widget_Material3_MaterialButtonGroup,
        intArrayOf(com.google.android.material.R.attr.buttonSizeChange),
      )

      val sizeChange = try {
        @Suppress("RestrictedApi")
        StateListSizeChange.create(
          context,
          attributes,
          0,
        )
      } finally {
        attributes.recycle()
      }

      if (sizeChange != null) {
        @Suppress("RestrictedApi")
        setButtonSizeChange(sizeChange)
      }
    }

    requestLayout()
  }
}