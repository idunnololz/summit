package com.idunnololz.summit.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.reddit_objects.AuthResponse
import com.idunnololz.summit.reddit_objects.UserInfo
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import okhttp3.*
import java.lang.RuntimeException
import java.security.SecureRandom

class RedditAuthManager(
    private val context: Context
) {

    companion object {

        private val TAG = RedditAuthManager::class.java.simpleName

        private const val CLIENT_ID = "rpj_7q3HnEwufA"
        private const val REDIRECT_URL = "http://summit.idunnololz.com/oauth"
        private const val DEFAULT_SCOPE = "identity,edit,flair,history,modconfig,modflair,modlog," +
                "modposts,modwiki,mysubreddits,privatemessages,read,report,save,submit,subscribe," +
                "vote,wikiedit,wikiread"

        @SuppressLint("StaticFieldLeak") // application context
        lateinit var instance: RedditAuthManager
            private set

        fun initialize(context: Context) {
            instance = RedditAuthManager(context.applicationContext)
        }
    }

    private val srand = SecureRandom()

    var authToken: String? =
        PreferenceUtil.preferences.getString(PreferenceUtil.KEY_OAUTH_TOKEN, null)
        private set

    fun showPreSignInIfNeeded(fragmentManager: FragmentManager): Boolean {
        if (authToken != null) return false

        PreAuthDialogFragment.newInstance()
            .show(fragmentManager, "asdf")

        return true
    }

    /**
     * Returns true if sign in was shown.
     */
    fun showSignInIfNeeded(): Boolean {
        if (authToken != null) return false

        val stateToken = generateStateToken()
        PreferenceUtil.preferences.edit()
            .putString(PreferenceUtil.KEY_STATE_TOKEN, stateToken)
            .apply()

        val uri = Uri.parse("https://www.reddit.com/api/v1/authorize.compact")
            .buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", stateToken)
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("duration", "permanent")
            .appendQueryParameter("scope", DEFAULT_SCOPE)
            .build()

        Utils.safeLaunchExternalIntent(context, Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        return true
    }

    private fun generateStateToken(): String {
        val randomBytes = ByteArray(128)
        srand.nextBytes(randomBytes)
        return Base64.encodeToString(
            randomBytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    fun handleAuthAttempt(
        data: Uri,
        supportFragmentManager: FragmentManager
    ) {
        Log.d(TAG, "handleAuthAttempt()")

        AuthDialogFragment.newInstance(data)
            .showAllowingStateLoss(supportFragmentManager, "auth")
    }

    fun fetchToken(code: String): Result<Any> {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URL)
            .build()

        val credentials = Credentials.basic(CLIENT_ID, "")
        val req = Request.Builder()
            .header("Authorization", credentials)
            .url("https://www.reddit.com/api/v1/access_token")
            .post(form)
            .build()

        val response = Client.get().newCall(req).execute()

        return if (response.code == 200) {
            val jsonResponse = response.body?.string()
            val authResponse: AuthResponse =
                Utils.gson.fromJson(jsonResponse, AuthResponse::class.java)

            val userInfo = fetchUserInfo()

            PreferenceUtil.preferences.edit()
                .putString(PreferenceUtil.KEY_OAUTH_TOKEN, authResponse.accessToken)
                .putString(PreferenceUtil.KEY_REFRESH_TOKEN, authResponse.refreshToken)
                .putString(PreferenceUtil.KEY_USER_ID, userInfo?.id)
                .apply()

            authToken = authResponse.accessToken

            Log.d(TAG, "fetchToken success!")

            Result.success(0)
        } else {
            Log.d(TAG, "fetchToken failure. Error: ${response.code}")

            Result.failure(RuntimeException("Fetch token failed. Error: ${response.code}"))
        }
    }

    fun refreshAccessToken(): String? {
        val refreshToken =
            PreferenceUtil.preferences.getString(PreferenceUtil.KEY_REFRESH_TOKEN, null)

        if (refreshToken == null) {
            PreferenceUtil.preferences.edit()
                .remove(PreferenceUtil.KEY_OAUTH_TOKEN)
                .apply()
            return null
        }

        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val credentials = Credentials.basic(CLIENT_ID, "")
        val req = Request.Builder()
            .header("Authorization", credentials)
            .url("https://www.reddit.com/api/v1/access_token")
            .post(form)
            .build()

        val response = Client.get().newCall(req).execute()

        return if (response.code == 200) {
            val jsonResponse = response.body?.string()
            val authResponse: AuthResponse =
                Utils.gson.fromJson(jsonResponse, AuthResponse::class.java)

            PreferenceUtil.preferences.edit()
                .putString(PreferenceUtil.KEY_OAUTH_TOKEN, authResponse.accessToken)
                .apply()

            authToken = authResponse.accessToken

            Log.d(TAG, "fetchToken success!")

            authToken
        } else {
            Log.d(TAG, "fetchToken failure. Error: ${response.code}")

            null
        }
    }

    fun makeAuthedCall(url: String): Response {
        val req = Request.Builder()
            .addHeader("Authorization", "Bearer $authToken")
            .url(url)
            .build()

        return Client.get().newCall(req).execute()
    }

    fun makeAuthedPost(url: String, formBody: FormBody): Response {
        val req = Request.Builder()
            .addHeader("Authorization", "Bearer $authToken")
            .url(url)
            .post(formBody)
            .build()

        return Client.get().newCall(req).execute()
    }

    fun getCachedUserInfo(): UserInfo? {
        val userId =
            PreferenceUtil.preferences.getString(PreferenceUtil.KEY_USER_ID, null) ?: return null
        val json = DataFiles.instance.getCachedData("user_info_${userId}")

        return Utils.gson.fromJson(json, UserInfo::class.java)
    }

    fun fetchUserInfo(): UserInfo? {
        val response = makeAuthedCall("https://oauth.reddit.com/api/v1/me.json")

        return if (response.isSuccessful) {
            val json = response.body?.string()
            val userInfo = Utils.gson.fromJson(json, UserInfo::class.java)

            if (userInfo != null) {
                DataFiles.instance.cacheData("user_info_${userInfo.id}", json)

                PreferenceUtil.preferences.edit()
                    .putString(PreferenceUtil.KEY_USER_ID, userInfo?.id)
                    .apply()
            }

            userInfo
        } else {
            null
        }
    }

    fun signOut() {
        val refreshToken =
            PreferenceUtil.preferences.getString(PreferenceUtil.KEY_REFRESH_TOKEN, null)

        if (refreshToken != null) {
            val form = FormBody.Builder()
                .add("token_type_hint", "refresh_token")
                .add("token", refreshToken)
                .build()

            val req = Request.Builder()
                .url("https://www.reddit.com/api/v1/revoke_token")
                .post(form)
                .build()

            // best effort
            Client.get().newCall(req).execute()
        }

        PreferenceUtil.preferences.edit()
            .remove(PreferenceUtil.KEY_OAUTH_TOKEN)
            .remove(PreferenceUtil.KEY_REFRESH_TOKEN)
            .remove(PreferenceUtil.KEY_USER_ID)
            .apply()

        authToken = null
    }
}