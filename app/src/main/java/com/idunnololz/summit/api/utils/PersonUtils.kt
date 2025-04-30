package com.idunnololz.summit.api.utils

import android.net.Uri
import com.idunnololz.summit.api.dto.Person

val Person.instance: String
  get() = Uri.parse(this.actor_id).host ?: this.actor_id

val Person.fullName: String
  get() = "$name@$instance"
