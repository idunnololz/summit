package com.idunnololz.summit.redirect

import android.util.Log
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.util.Client
import com.idunnololz.summit.util.StatefulLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.Request

class RedirectHandlerViewModel : ViewModel() {

    companion object {
        private val TAG = RedirectHandlerViewModel::class.java.simpleName
    }

    val redirectResult = StatefulLiveData<RedirectResult>()

    private val disposables = CompositeDisposable()

    data class RedirectResult(
        val finalUrl: String
    )

    fun resolveRedirectUrl(url: String) {
        disposables.clear()

        redirectResult.setIsLoading()

        val client = Client.get()

        val d = Single
            .fromCallable {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Chrome")
                    .build()
                val response = client.newCall(request).execute()
                response.request.url.toString()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ finalUrl ->
                Log.d(TAG, "URL: $finalUrl")

                redirectResult.setValue(RedirectResult(finalUrl.toString()))
            }, {
                redirectResult.setError(it)
            })

        disposables.add(d)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}