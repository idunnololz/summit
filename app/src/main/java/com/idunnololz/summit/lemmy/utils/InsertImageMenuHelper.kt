package com.idunnololz.summit.lemmy.utils

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.ext.showAllowingStateLoss

fun BaseFragment<*>.showInsertImageMenu(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
) {
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
                            arguments =
                                ChooseSavedImageDialogFragmentArgs().toBundle()
                        }
                        .showAllowingStateLoss(
                            childFragmentManager,
                            "ChooseSavedImageDialogFragment",
                        )
                }
            }
        }
    }

    requireMainActivity().showBottomMenu(bottomMenu)
}