package com.idunnololz.summit.avatar

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
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
        loadAvatar(
            imageView = imageView,
            imageUrl = person.avatar,
            personName = person.name,
            personId = person.id,
            personInstance = person.instance,
        )
    }

    fun generateDrawableAndSet(
        view: View,
        personName: String,
        personId: PersonId,
        personInstance: String,
        setDrawable: (Drawable) -> Unit,
    ) {
        (view.getTag(R.id.generate_profile_icon_job) as Job?)?.cancel()
        val job = coroutineScope.launch {
            val d = accountImageGenerator.generateDrawableForPerson(
                personName,
                personId,
                personInstance,
            )

            withContext(Dispatchers.Main) {
                setDrawable(d)
            }
        }
        view.setTag(R.id.generate_profile_icon_job, job)
    }

    fun loadAvatar(
        imageView: ImageView,
        imageUrl: String?,
        personName: String,
        personId: PersonId,
        personInstance: String,
    ) {
        (imageView.getTag(R.id.generate_profile_icon_job) as Job?)?.cancel()

        if (imageUrl.isNullOrBlank()) {
            val job = coroutineScope.launch {
                val d = accountImageGenerator.generateDrawableForPerson(
                    personName,
                    personId,
                    personInstance,
                )

                withContext(Dispatchers.Main) {
                    imageView.dispose()
                    imageView.setImageDrawable(d)
                }
            }
            imageView.setTag(R.id.generate_profile_icon_job, job)
        } else {
            imageView.load(imageUrl) {
                placeholder(newShimmerDrawableSquare(context))
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
                placeholder(newShimmerDrawableSquare(context))
                allowHardware(false)
            }
        }
    }
}
