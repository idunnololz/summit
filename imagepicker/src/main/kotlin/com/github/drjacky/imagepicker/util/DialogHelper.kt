package com.github.drjacky.imagepicker.util

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.github.drjacky.imagepicker.R
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.github.drjacky.imagepicker.databinding.DialogChooseAppBinding
import com.github.drjacky.imagepicker.listener.DismissListener
import com.github.drjacky.imagepicker.listener.ResultListener

/**
 * Show Dialog
 *
 * @author Dhaval Patel
 * @version 1.0
 * @since 04 January 2018
 */
internal object DialogHelper {

    /**
     * Show Image Provide Picker Dialog. This will streamline the code to pick/capture image
     *
     */
    fun showChooseAppDialog(
        context: Context,
        listener: ResultListener<ImageProvider>,
        dismissListener: DismissListener?,
    ) {
        val layoutInflater = LayoutInflater.from(context)
        val binding = DialogChooseAppBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.title_choose_image_provider)
            .setView(binding.root)
            .setOnCancelListener {
                listener.onResult(null)
            }
            .setOnDismissListener {
                dismissListener?.onDismiss()
                listener.onResult(null)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                listener.onResult(null)
            }
            .show()

        // Handle Camera option click
        binding.lytCameraPick.setOnClickListener {
            listener.onResult(ImageProvider.CAMERA)
            dialog.dismiss()
        }

        // Handle Gallery option click
        binding.lytGalleryPick.setOnClickListener {
            listener.onResult(ImageProvider.GALLERY)
            dialog.dismiss()
        }
    }
}
