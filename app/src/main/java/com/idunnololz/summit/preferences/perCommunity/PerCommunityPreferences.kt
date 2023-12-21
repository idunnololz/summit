package com.idunnololz.summit.preferences.perCommunity

import android.content.Context
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.community.CommunityLayout
import com.idunnololz.summit.util.ext.getMoshiValue
import com.idunnololz.summit.util.ext.putMoshiValue
import com.squareup.moshi.JsonClass
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerCommunityPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val preferences = context.getSharedPreferences("pcp", Context.MODE_PRIVATE)

    fun getCommunityConfig(communityRef: CommunityRef): CommunityConfig? {
        return getCommunityConfig(communityRef.getKey())
    }

    fun getCommunityConfig(key: String): CommunityConfig? {
        return preferences.getMoshiValue<CommunityConfig>(key)
    }

    fun setCommunityConfig(communityRef: CommunityRef, config: CommunityConfig?) {
        preferences.putMoshiValue(communityRef.getKey(), config)
    }

    fun getAllCommunityConfigs(): List<CommunityConfig> =
        preferences.all.keys.mapNotNull {
            getCommunityConfig(it)
        }

    fun clear() {
        preferences.edit().clear().apply()
    }

    @JsonClass(generateAdapter = true)
    data class CommunityConfig(
        val communityRef: CommunityRef,
        val layout: CommunityLayout? = null,
    )
}
