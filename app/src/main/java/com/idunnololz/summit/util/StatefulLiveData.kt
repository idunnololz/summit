package com.idunnololz.summit.util

import androidx.annotation.MainThread
import androidx.datastore.preferences.protobuf.BoolValueOrBuilder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class StatefulLiveData<T> {

    private val data = MutableLiveData<StatefulData<T>>(StatefulData.NotStarted())

    val value: StatefulData<T>
        get() {
            return requireNotNull(data.value)
        }

    val isLoading: Boolean
        get() {
            val value = value
            return value is StatefulData.Loading
        }

    val isLoaded: Boolean
        get() {
            val value = value
            return value is StatefulData.Success || value is StatefulData.Error
        }

    val isNotStarted: Boolean
        get() {
            val value = value
            return value is StatefulData.NotStarted
        }

    val valueOrNull: T?
        get() {
            val value = value

            return if (value is StatefulData.Success) {
                value.data
            } else {
                null
            }
        }

    @MainThread
    fun setValue(value: T) {
        data.value = StatefulData.Success(value)
    }

    fun postValue(value: T) {
        data.postValue(StatefulData.Success(value))
    }

    @MainThread
    fun setIsLoading(
        statusDesc: String? = null,
        progress: Int = 0,
        maxProgress: Int = 0,
        payload: T? = null,
    ) {
        data.value = StatefulData.Loading(
            statusDesc = statusDesc,
            progress = progress,
            maxProgress = maxProgress,
            payload = payload,
        )
    }
    fun postIsLoading(
        statusDesc: String? = null,
        progress: Int = 0,
        maxProgress: Int = 0,
        isIndeterminate: Boolean = false,
        payload: T? = null,
    ) {
        data.postValue(
            StatefulData.Loading(
                statusDesc = statusDesc,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
                payload = payload,
            ),
        )
    }

    @Suppress("unused")
    fun postIsLoading(
        statusDesc: String? = null,
        progress: Int = 0,
        maxProgress: Int = 0,
        isIndeterminate: Boolean = false,
    ) {
        data.postValue(
            StatefulData.Loading(
                statusDesc = statusDesc,
                progress = progress,
                maxProgress = maxProgress,
                isIndeterminate = isIndeterminate,
            ),
        )
    }

    @MainThread
    fun setIdle() {
        data.value = StatefulData.NotStarted()
    }

    @MainThread
    fun postIdle() {
        data.postValue(StatefulData.NotStarted())
    }

    @MainThread
    fun setError(error: Throwable) {
        data.value = StatefulData.Error(error)
    }

    fun postError(error: Throwable) {
        data.postValue(StatefulData.Error(error))
    }

    fun clear() {
        data.value = StatefulData.NotStarted()
    }

    @MainThread
    fun observe(owner: LifecycleOwner, observer: Observer<StatefulData<T>>) {
        data.observe(owner, observer)
    }

    @MainThread
    @Suppress("unused")
    fun observeForever(observer: Observer<StatefulData<T>>) {
        data.observeForever(observer)
    }

    @MainThread
    @Suppress("unused")
    fun removeObserver(observer: Observer<StatefulData<T>>) {
        data.removeObserver(observer)
    }

    @MainThread
    @Suppress("unused")
    fun removeObservers(owner: LifecycleOwner) {
        data.removeObservers(owner)
    }
}

sealed class StatefulData<T> {
    class NotStarted<T> : StatefulData<T>()
    data class Loading<T>(
        val statusDesc: String? = null,
        val progress: Int = 0,
        val maxProgress: Int = 0,
        val payload: T? = null,
        val isIndeterminate: Boolean = true,
    ) : StatefulData<T>() {
        fun requirePayload(): T = requireNotNull(payload)
    }

    class Success<T>(val data: T) : StatefulData<T>()
    class Error<T>(val error: Throwable) : StatefulData<T>()
}

fun StatefulData<*>.isLoading() = this is StatefulData.Loading<*>
