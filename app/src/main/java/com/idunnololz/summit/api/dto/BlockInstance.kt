package com.idunnololz.summit.api.dto

data class BlockInstance(
    val instance_id: InstanceId,
    val block: Boolean,
    val auth: String,
)
