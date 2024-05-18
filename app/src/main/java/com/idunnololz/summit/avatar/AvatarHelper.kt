package com.idunnololz.summit.avatar

import android.content.Context
import android.widget.ImageView
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.ext.getDrawableCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AvatarHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountImageGenerator: AccountImageGenerator,
    coroutineScopeFactory: CoroutineScopeFactory,
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

    fun loadIcon(imageView: ImageView, community: Community) {
        (imageView.getTag(R.id.generate_community_icon_job) as Job?)?.cancel()

        if (community.icon.isNullOrBlank()) {
            val job = coroutineScope.launch {
                val d = accountImageGenerator.generateDrawableForGeneric(
                    community.fullName,
                    context.getDrawableCompat(R.drawable.ic_lemmy_outline_community_icon_24),
                )

                withContext(Dispatchers.Main) {
                    imageView.dispose()
                    imageView.setImageDrawable(d)
                }
            }
            imageView.setTag(R.id.generate_profile_icon_job, job)
        } else {
            imageView.load(community.icon) {
                placeholder(R.drawable.thumbnail_placeholder_square)
                allowHardware(false)
            }
        }
    }
}
