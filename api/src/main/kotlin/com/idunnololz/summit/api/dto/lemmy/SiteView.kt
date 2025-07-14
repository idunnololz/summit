package com.idunnololz.summit.api.dto.lemmy

data class SiteView(
  val site: Site,
  val local_site: LocalSite,
  val local_site_rate_limit: LocalSiteRateLimit? = null,
  val counts: SiteAggregates,
)
