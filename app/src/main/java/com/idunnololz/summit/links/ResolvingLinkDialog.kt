package com.idunnololz.summit.links

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.databinding.DialogResolvingLinkBinding

class ResolvingLinkDialog {

    companion object {
        fun show(context: Context): AlertDialog? {
            return MaterialAlertDialogBuilder(context)
                .apply {
                    val binding = DialogResolvingLinkBinding.inflate(
                        LayoutInflater.from(context),
                    )
                    setView(binding.root)
                }
                .show()
        }
    }
}
