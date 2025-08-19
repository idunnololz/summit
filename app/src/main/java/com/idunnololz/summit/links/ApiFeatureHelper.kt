package com.idunnololz.summit.links

import com.idunnololz.summit.api.ApiFeature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiFeatureHelper @Inject constructor(
  private val siteBackendHelper: SiteBackendHelper,
) {
  fun instanceSupportsFeature(instance: String, feature: ApiFeature, defaultValue: Boolean) =
    siteBackendHelper.getApiInfoFromCache(instance)?.supportsFeature(feature) ?: defaultValue

  suspend fun instanceSupportsFeatureAsync(
    instance: String,
    feature: ApiFeature,
    defaultValue: Boolean,
  ) =
    siteBackendHelper.fetchApiInfo(instance).getOrNull()?.supportsFeature(feature) ?: defaultValue
}

fun ApiFeatureHelper.supportsDownvotes(instance: String): Boolean =
  instanceSupportsFeature(instance = instance, feature = ApiFeature.Downvote, defaultValue = true)