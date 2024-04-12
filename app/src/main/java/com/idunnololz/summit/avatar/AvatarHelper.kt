package com.idunnololz.summit.avatar

import android.widget.ImageView
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarHelper @Inject constructor(
    private val accountImageGenerator: AccountImageGenerator,
    coroutineScopeFactory: CoroutineScopeFactory
) {

    private val coroutineScope = coroutineScopeFactory.create()

    fun loadAvatar(imageView: ImageView, person: Person) {
        (imageView.getTag(R.id.generate_profile_icon_job) as Job?)?.cancel()

        if (person.avatar.isNullOrBlank()) {
            val job = coroutineScope.launch {
                val d = accountImageGenerator.generateDrawableForPerson(person)

                withContext(Dispatchers.Main) {
                    imageView.dispose()
                    imageView.setImageDrawable(d)
                }
            }
            imageView.setTag(R.id.generate_profile_icon_job, job)
        } else {
            imageView.load(person.avatar) {
                placeholder(R.drawable.thumbnail_placeholder_square)
                allowHardware(false)
            }
        }
    }
}