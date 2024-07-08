package com.idunnololz.summit.util

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.CouldntFindObjectError
import com.idunnololz.summit.api.NetworkException
import com.idunnololz.summit.api.NewApiException
import com.idunnololz.summit.api.NoInternetException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.api.ServerTimeoutException
import com.idunnololz.summit.api.SocketTimeoutException
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource
import com.idunnololz.summit.lemmy.multicommunity.NoModeratedCommunitiesException
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.scrape.WebsiteAdapterLoader

private const val TAG = "ErrorUtils"

fun Throwable.toErrorMessage(context: Context): String {
    return when (val t = this) {
        is LoaderException ->
            context.getString(WebsiteAdapterLoader.getDefaultErrorMessageFor(t.errorCode))
        is ApiException ->
            when (t) {
                is ClientApiException -> {
                    Log.e(TAG, "Unknown throwable ${t::class.java.canonicalName}", t)
                    when (t) {
                        is ServerTimeoutException -> {
                            context.getString(R.string.error_server_timeout)
                        }

                        is NotAuthenticatedException -> {
                            context.getString(R.string.error_not_signed_in)
                        }

                        is NewApiException -> {
                            context.getString(R.string.error_new_api_format, t.minVersion)
                        }

                        is CouldntFindObjectError -> {
                            context.getString(R.string.error_couldnt_find_object)
                        }

                        else -> {
                            if (t.errorCode == 404) {
                                context.getString(R.string.error_page_not_found)
                            } else {
                                FirebaseCrashlytics.getInstance().recordException(t)
                                context.getString(R.string.error_unknown)
                            }
                        }
                    }
                }
                is ServerApiException ->
                    context.getString(R.string.error_server, t.errorCode.toString())
            }
        is NetworkException ->
            when (t) {
                is SocketTimeoutException ->
                    context.getString(R.string.error_socket_timeout)

                is NoInternetException ->
                    context.getString(R.string.error_network)
            }
        is MultiCommunityDataSource.CommunityNotFoundException ->
            context.getString(
                R.string.error_community_not_found_on_instance,
                t.communityRef.fullName,
            )
        is NoModeratedCommunitiesException ->
            context.getString(R.string.error_no_moderated_communities)
        else -> {
            Log.e(TAG, "Unknown throwable ${t::class.java.canonicalName}", t)
            context.getString(R.string.error_unknown)
        }
    }
}
