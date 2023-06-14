package com.idunnololz.summit.scrape

class LoaderException(
    val errorCode: Int
) : Exception("Failed with error: $errorCode")