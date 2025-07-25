package com.idunnololz.summit.api.utils

import android.net.Uri
import com.idunnololz.summit.api.dto.lemmy.Community

val Community.instance: String
  get() = Uri.parse(this.actor_id).host ?: this.actor_id

val Community.fullName: String
  get() = "$name@$instance"
