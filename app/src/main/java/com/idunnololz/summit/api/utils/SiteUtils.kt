package com.idunnololz.summit.api.utils

import android.net.Uri
import com.idunnololz.summit.api.dto.lemmy.Site

val Site.instance: String
  get() = Uri.parse(this.actor_id).host ?: this.actor_id
