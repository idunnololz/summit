package com.idunnololz.summit.image

data class ImageInfoModel(
    val items: List<Item>
) {

    sealed interface Item {
    }
    data class InfoItem(
        val title: String,
        val value: String,
    ): Item
}