package com.idunnololz.summit.preferences.perCommunity

import android.content.Context
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.util.ext.getJsonValue
import com.idunnololz.summit.util.ext.putJsonValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class PerCommunityPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val preferences = context.getSharedPreferences("pcp", Context.MODE_PRIVATE)

    fun getCommunityConfig(communityRef: CommunityRef): CommunityConfig? {
        return getCommunityConfig(communityRef.getKey())
    }

    fun getCommunityConfig(key: String): CommunityConfig? {
        return preferences.getJsonValue<CommunityConfig>(json, key)
    }

    fun setCommunityConfig(communityRef: CommunityRef, config: CommunityConfig?) {
        preferences.putJsonValue(json, communityRef.getKey(), config)
    }

    fun getAllCommunityConfigs(): List<CommunityConfig> = preferences.all.keys.mapNotNull {
        getCommunityConfig(it)
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    @Serializable
    data class CommunityConfig(
        val communityRef: CommunityRef,
        val layout: CommunityLayout? = null,
        val sortOrder: CommunitySortOrder? = null,
    )
}
