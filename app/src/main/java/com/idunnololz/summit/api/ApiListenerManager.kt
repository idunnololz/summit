package com.idunnololz.summit.api

import android.util.Log
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

typealias ApiListener = (Response<*>) -> Unit

@Singleton
class ApiListenerManager @Inject constructor(

) {
    private var listeners = listOf<ApiListener>()

    fun onRequestComplete(response: Response<*>) {
        val listeners = listeners
        for (listener in listeners) {
            listener(response)
        }
    }

    fun registerListener(listener: ApiListener) {
        listeners += listener
    }
}