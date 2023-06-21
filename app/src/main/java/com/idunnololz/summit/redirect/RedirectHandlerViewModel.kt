package com.idunnololz.summit.redirect

import android.util.Log
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.util.Client
import com.idunnololz.summit.util.StatefulLiveData
import okhttp3.Request

class RedirectHandlerViewModel : ViewModel() {

    companion object {
        private val TAG = RedirectHandlerViewModel::class.java.simpleName
    }

    val redirectResult = StatefulLiveData<RedirectResult>()

    data class RedirectResult(
        val finalUrl: String
    )

    fun resolveRedirectUrl(url: String) {
//        disposables.clear()
//
//        redirectResult.setIsLoading()
//
//        val client = Client.get()
//
//        val d = Single
//            .fromCallable {
//                val request = Request.Builder()
//                    .url(url)
//                    .header("User-Agent", "Chrome")
//                    .build()
//                val response = client.newCall(request).execute()
//                response.request.url.toString()
//            }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({ finalUrl ->
//                Log.d(TAG, "URL: $finalUrl")
//
//                redirectResult.setValue(RedirectResult(finalUrl.toString()))
//            }, {
//                redirectResult.setError(it)
//            })
//
//        disposables.add(d)
    }
}