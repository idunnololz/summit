package com.idunnololz.summit.preferences

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.lemmy.post_view.PostUiConfig
import com.idunnololz.summit.lemmy.post_view.getDefaultPostUiConfig
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

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
        return try {
            val json = prefs.getString(getPostUiConfigKey(), null)
                ?: return getPostsLayout().getDefaultPostUiConfig()
            moshi.adapter(PostUiConfig::class.java).fromJson(
                json
            ) ?: getPostsLayout().getDefaultPostUiConfig()
        } catch (e: Exception) {
            getPostsLayout().getDefaultPostUiConfig()
        }
    }

    fun setPostUiConfig(config: PostUiConfig) {
        prefs.edit()
            .putString(getPostUiConfigKey(),
                moshi.adapter(PostUiConfig::class.java).toJson(config))
            .apply()
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
}