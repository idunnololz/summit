package com.idunnololz.summit.settings

import androidx.lifecycle.ViewModel
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.reddit_objects.UserInfo
import com.idunnololz.summit.util.StatefulLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException

class UserInfoViewModel : ViewModel() {

    val disposables = CompositeDisposable()

    val userInfoLiveData = StatefulLiveData<UserInfo>()

    val signOutLiveData = StatefulLiveData<Unit>()

    fun fetchUserInfo() {
        userInfoLiveData.setIsLoading()

        val d = Single
            .fromCallable {
                RedditAuthManager.instance.fetchUserInfo()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ userInfo ->
                if (userInfo != null) {
                    userInfoLiveData.setValue(userInfo)
                } else {
                    userInfoLiveData.setError(RuntimeException())
                }
            }, {
                userInfoLiveData.setError(RuntimeException())
            })

        disposables.add(d)
    }

    fun signOut() {
        signOutLiveData.setIsLoading()

        val d = Single
            .fromCallable {
                RedditAuthManager.instance.signOut()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ userInfo ->
                if (userInfo != null) {
                    signOutLiveData.setValue(Unit)
                } else {
                    signOutLiveData.setError(RuntimeException())
                }
            }, {
                signOutLiveData.setError(RuntimeException())
            })

        disposables.add(d)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}