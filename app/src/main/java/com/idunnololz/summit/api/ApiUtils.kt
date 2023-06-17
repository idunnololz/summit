package com.idunnololz.summit.api

import android.util.Log
import org.json.JSONObject
import retrofit2.Response

fun <T> retrofitErrorHandler(res: Response<T>): T {
    if (res.isSuccessful) {
        return res.body()!!
    } else {
        val errorBody = res.errorBody()?.string()
        val errMsg = try {
            errorBody?.let {
                JSONObject(it).getString("error")
            } ?: run {
                res.code().toString()
            }
        } catch (e: Exception) {
            errorBody
        }
        Log.e("ApiError", "Error message: ${errMsg}", RuntimeException())

        throw Exception(errMsg)
    }
}