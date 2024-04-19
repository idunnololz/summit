package com.idunnololz.summit.links

import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.moshi

object LinkResolver {

    fun parseUrl(url: String, currentInstance: String, mustHandle: Boolean = false): PageRef? {
        val normalizedUrl = normalizeUrl(url, currentInstance)
            ?: return null
        return parseUrlInternal(normalizedUrl, mustHandle)
    }

    private fun normalizeUrl(url: String, currentInstance: String): String? {
        if (url.startsWith("https://")) {
            return url
        } else if (url.startsWith("/c/")) {
            if (url.count { c -> c == '@' } == 1) {
                val (community, host) = url.split("@", limit = 2)
                return "https://$host$community"
            }
            return "https://${currentInstance}$url"
        } else if (url.startsWith("/u/")) {
            if (url.count { c -> c == '@' } == 1) {
                val (userPath, host) = url.split("@", limit = 2)
                return "https://$host$userPath"
            }
            return "https://${currentInstance}$url"
        } else if (url.startsWith("!")) {
            if (url.count { c -> c == '@' } == 1) {
                val (community, host) = url.substring(1).split("@", limit = 2)
                return "https://$host/c/$community"
            }
            return "https://$currentInstance/c/${url.substring(1)}"
        } else if (url.startsWith("@")) {
            if (url.count { c -> c == '@' } == 2) {
                val (user, host) = url.substring(1).split("@", limit = 2)
                return "https://$host/u/$user"
            }
            return "https://$currentInstance/u/${url.substring(1)}"
        }
        return null
    }

    private fun parseUrlInternal(url: String, mustHandle: Boolean): PageRef? {
        if (!url.startsWith("https://")) {
            return null
        }

        val uri = Uri.parse(url)
        val instance = uri.host ?: return null
        val defaultResult = CommunityRef.Local(instance)

        when (uri.pathSegments.firstOrNull()) {
            null -> { // This is likely a link to the front page
                val listingType = uri.getQueryParameter("listingType")
                if (listingType != null) {
                    if (listingType.equals("all", ignoreCase = true)) {
                        return CommunityRef.All(instance)
                    } else if (listingType.equals("local", ignoreCase = true)) {
                        return CommunityRef.Local(instance)
                    } else if (listingType.equals("subscribed", ignoreCase = true)) {
                        return CommunityRef.Subscribed(instance)
                    } else if (listingType.equals("moderatorview", ignoreCase = true)) {
                        return CommunityRef.ModeratedCommunities(instance)
                    }
                }

                val fragment = uri.fragment
                if (fragment != null && fragment.startsWith("!")) {
                    // this is likely a multi-community url
                    val communityRef: CommunityRef? = try {
                        moshi.adapter(CommunityRef::class.java).fromJson(fragment.drop(1))
                    } catch (e: Exception) {
                        // best effort
                        null
                    }

                    if (communityRef != null) {
                        return communityRef
                    }
                }

                if (mustHandle) {
                    return defaultResult
                } else {
                    return null
                }
            }
            "c" -> { // link to a community
                var communityName = uri.pathSegments.getOrNull(1)
                    ?: return defaultResult

                communityName = communityName.trimEnd { it == '.' }

                return if (communityName.count { c -> c == '@' } == 1) {
                    val (community, instance) = url.substring(1).split("@", limit = 2)
                    CommunityRef.CommunityRefByName(community, instance)
                } else {
                    CommunityRef.CommunityRefByName(communityName, instance)
                }
            }
            "u" -> {
                var personName = uri.pathSegments.getOrNull(1)
                    ?: return defaultResult

                personName = personName.trimEnd { it == '.' }

                return if (personName.count { c -> c == '@' } == 1) {
                    val (personName, instance) = url.substring(1).split("@", limit = 2)
                    PersonRef.PersonRefByName(personName, instance)
                } else {
                    PersonRef.PersonRefByName(personName, instance)
                }
            }
            "post" -> {
                val postIdStr = uri.pathSegments.getOrNull(1)
                    ?: return defaultResult
                val postId = postIdStr.toIntOrNull()

                if (postIdStr.isNotBlank() && postId == null) {
                    return null
                }

                return PostRef(instance, postId ?: return defaultResult)
            }
            "comment" -> {
                val commentId = uri.pathSegments.getOrNull(1)
                    ?: return defaultResult
                return CommentRef(instance, commentId.toIntOrNull() ?: return defaultResult)
            }
            else -> {
                if (mustHandle) {
                    FirebaseCrashlytics.getInstance().recordException(
                        RuntimeException("Unrecognized url format: $url"),
                    )
                    return defaultResult
                } else {
                    return null
                }
            }
        }
    }
}
