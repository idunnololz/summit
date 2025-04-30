package com.idunnololz.summit.avatar

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import coil3.asImage
import coil3.dispose
import coil3.imageLoader
import coil3.load
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.SiteView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
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
        placeholder(newShimmerDrawableSquare(context).asImage())
        allowHardware(false)
      }
    }
  }

  fun loadAvatar(
    target: coil3.target.Target,
    imageUrl: String?,
    personName: String,
    personId: PersonId,
    personInstance: String,
  ): Job {
    return if (imageUrl.isNullOrBlank()) {
      coroutineScope.launch {
        val d = accountImageGenerator.generateDrawableForPerson(
          personName = personName,
          personId = personId,
          personInstance = personInstance,
          circleClip = true,
        )

        withContext(Dispatchers.Main) {
          target.onSuccess(d.asImage())
        }
      }
    } else {
      coroutineScope.launch {
        context.imageLoader.execute(
          ImageRequest.Builder(context)
            .data(imageUrl)
            .transformations(CircleCropTransformation())
            .target(target)
            .placeholder(newShimmerDrawableSquare(context).asImage())
            .allowHardware(false)
            .build(),
        )
      }
    }
  }

  fun loadCommunityIcon(imageView: ImageView, community: Community) {
    loadCommunityIcon(imageView, community.toCommunityRef(), community.icon)
  }

  fun loadCommunityIcon(imageView: ImageView, communityRef: CommunityRef, iconUrl: String?) {
    (imageView.getTag(R.id.generate_community_icon_job) as Job?)?.cancel()

    if (iconUrl.isNullOrBlank()) {
      val job = coroutineScope.launch {
        val d = accountImageGenerator.generateDrawableForGeneric(
          communityRef.getKey(),
          context.getDrawableCompat(R.drawable.ic_lemmy_outline_community_icon_24),
        )

        withContext(Dispatchers.Main) {
          imageView.dispose()
          imageView.setImageDrawable(d)
        }
      }
      imageView.setTag(R.id.generate_profile_icon_job, job)
    } else {
            /*

                    offlineManager.fetchImageWithError(
                        rootView = h.itemView,
                        url = community.community.icon,
                        listener = {
                            b.icon.load(it)
                        },
                        errorListener = {
                            b.icon.load(R.drawable.ic_community_default)
                        },
                    )
             */
      imageView.load(iconUrl) {
        allowHardware(false)
        placeholder(newShimmerDrawableSquare(context).asImage())
        listener(
          onError = { _, _ ->
            loadCommunityIcon(imageView, communityRef, null)
          },
        )
      }
    }
  }

  fun loadInstanceIcon(imageView: ImageView, siteView: SiteView?) {
    loadInstanceIcon(imageView, siteView, siteView?.site?.icon)
  }

  fun loadInstanceIcon(imageView: ImageView, siteView: SiteView?, iconUrl: String?) {
    (imageView.getTag(R.id.generate_community_icon_job) as Job?)?.cancel()
    if (iconUrl.isNullOrBlank()) {
      val job = coroutineScope.launch {
        val d = accountImageGenerator.generateDrawableForGeneric(
          key = siteView?.site?.public_key ?: "",
          drawable = context.getDrawableCompat(
            R.drawable.ic_lemmy_outline_community_icon_24,
          ),
        )

        withContext(Dispatchers.Main) {
          imageView.dispose()
          imageView.setImageDrawable(d)
        }
      }
      imageView.setTag(R.id.generate_profile_icon_job, job)
    } else {
      imageView.load(iconUrl) {
        allowHardware(false)
        placeholder(newShimmerDrawableSquare(context).asImage())
        listener(
          onError = { _, _ ->
            loadInstanceIcon(imageView, siteView, null)
          },
        )
      }
    }
  }
}
