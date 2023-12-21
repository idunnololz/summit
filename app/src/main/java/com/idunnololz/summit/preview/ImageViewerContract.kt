package com.idunnololz.summit.preview

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper

class ImageViewerContract : ActivityResultContract<ImageViewerActivityArgs, Int?>() {
    @CallSuper
    override fun createIntent(context: Context, input: ImageViewerActivityArgs): Intent {
        val intent = Intent(context, ImageViewerActivity::class.java).apply {
            putExtras(input.toBundle())
        }
        return intent
    }

    final override fun getSynchronousResult(
        context: Context,
        input: ImageViewerActivityArgs,
    ): SynchronousResult<Int?>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): Int? {
        return resultCode
    }
}
