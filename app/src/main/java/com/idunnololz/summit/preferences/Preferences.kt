package com.idunnololz.summit.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.post_view.PostUiConfig
import com.idunnololz.summit.lemmy.post_view.getDefaultPostUiConfig
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.PreferenceUtil.KEY_BASE_THEME
import com.idunnololz.summit.util.moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "Preferences"
    }

    private val prefs = PreferenceUtil.preferences

    fun getDefaultPage(): CommunityRef {
        val communityJson = prefs.getString(PreferenceUtil.KEY_DEFAULT_PAGE, null)
        val communityRef = if (communityJson == null) {
            null
        } else {
            try {
                moshi.adapter(CommunityRef::class.java)
                    .fromJson(communityJson)
            } catch (e: Exception) {
                null
            }
        }

        return communityRef ?: CommunityRef.All()
    }

    fun setDefaultPage(communityRef: CommunityRef) {
        prefs.edit()
            .putString(
                PreferenceUtil.KEY_DEFAULT_PAGE,
                moshi.adapter(CommunityRef::class.java).toJson(communityRef))
            .apply()
    }


    fun getPostsLayout(): CommunityLayout =
        try {
            CommunityLayout.valueOf(
                PreferenceUtil.preferences
                    .getString(PreferenceUtil.KEY_SUBREDDIT_LAYOUT, null) ?: ""
            )
        } catch (e: IllegalArgumentException) {
            CommunityLayout.List
        }

    fun setPostsLayout(layout: CommunityLayout) {
        PreferenceUtil.preferences.edit()
            .putString(PreferenceUtil.KEY_SUBREDDIT_LAYOUT, layout.name)
            .apply()
    }

    fun getPostUiConfig(): PostUiConfig {
        return prefs.getMoshiValue<PostUiConfig>(getPostUiConfigKey())
            ?: getPostsLayout().getDefaultPostUiConfig()
    }

    fun setPostUiConfig(config: PostUiConfig) {
        prefs.putMoshiValue(getPostUiConfigKey(), config)
    }

    private fun getPostUiConfigKey() =
        when (getPostsLayout()) {
            CommunityLayout.Compact ->
                PreferenceUtil.KEY_POST_UI_CONFIG_COMPACT
            CommunityLayout.List ->
                PreferenceUtil.KEY_POST_UI_CONFIG_LIST
            CommunityLayout.Card ->
                PreferenceUtil.KEY_POST_UI_CONFIG_CARD
            CommunityLayout.Full ->
                PreferenceUtil.KEY_POST_UI_CONFIG_FULL
        }

    fun getBaseTheme(): BaseTheme {
        return prefs.getMoshiValue<BaseTheme>(KEY_BASE_THEME)
            ?: BaseTheme.Dark
    }

    fun setBaseTheme(baseTheme: BaseTheme) {
        prefs.putMoshiValue(KEY_BASE_THEME, baseTheme)
    }

    fun isUseMaterialYou(): Boolean {
        return prefs.getBoolean(PreferenceUtil.KEY_USE_MATERIAL_YOU, false)
    }

    fun setUseMaterialYou(b: Boolean) {
        prefs.edit().putBoolean(PreferenceUtil.KEY_USE_MATERIAL_YOU, b).apply()
    }

    private inline fun <reified T> SharedPreferences.getMoshiValue(key: String): T? {
        return try {
            val json = this.getString(key, null)
                ?: return null
            moshi.adapter(T::class.java).fromJson(
                json
            )
        } catch (e: Exception) {
            Log.e(TAG, "", e)
            null
        }
    }

    private inline fun <reified T> SharedPreferences.putMoshiValue(key: String, value: T) {
        this.edit()
            .putString(key, moshi.adapter(T::class.java).toJson(value))
            .apply()
    }
}