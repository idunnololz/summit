package com.idunnololz.summit.util

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class OpenImageDocument : ActivityResultContracts.OpenDocument() {

    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            type = "image/*"
        }
    }
}