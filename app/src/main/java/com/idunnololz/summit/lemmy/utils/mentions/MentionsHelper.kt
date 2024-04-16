package com.idunnololz.summit.lemmy.utils.mentions

import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.view.CustomTextInputEditText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MentionsHelper @Inject constructor(
    private val mentionsControllerFactory: MentionsController.Factory,
) {

    fun installMentionsSupportOn(
        lifecycleOwner: LifecycleOwner,
        customTextInputEditText: CustomTextInputEditText,
    ) {
        val controller = mentionsControllerFactory.create(lifecycleOwner, customTextInputEditText)
        controller.setup()
    }
}
