package com.idunnololz.summit.util

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class StatefulLiveData<T> {

    private val data = MutableLiveData<DataWithState<T>>()

    val value: DataWithState<T>?
        get() {
            return data.value
        }

    val isLoaded: Boolean
        get() {
            val value = value ?: return false
            return value.status != Status.LOADING
        }

    @MainThread
    fun setValue(value: T) {
        data.value = DataWithState(status = Status.SUCCESS, data = value)
    }

    fun postValue(value: T) {
        data.postValue(DataWithState(status = Status.SUCCESS, data = value))
    }

    @MainThread
    fun setIsLoading() {
        data.value = DataWithState(status = Status.LOADING)
    }

    @Suppress("unused")
    fun postIsLoading() {
        data.postValue(DataWithState(status = Status.LOADING))
    }

    @MainThread
    fun setError(error: Throwable) {
        data.value = DataWithState(status = Status.FAILED, error = error)
    }

    fun postError(error: Throwable) {
        data.postValue(DataWithState(status = Status.FAILED, error = error))
    }

    @MainThread
    fun observe(owner: LifecycleOwner, observer: Observer<DataWithState<T>>) {
        data.observe(owner, observer)
    }

    @MainThread
    @Suppress("unused")
    fun observeForever(observer: Observer<DataWithState<T>>) {
        data.observeForever(observer)
    }

    @MainThread
    @Suppress("unused")
    fun removeObserver(observer: Observer<DataWithState<T>>) {
        data.removeObserver(observer)
    }

    @MainThread
    @Suppress("unused")
    fun removeObservers(owner: LifecycleOwner) {
        data.removeObservers(owner)
    }
}

class DataWithState<T>(
    val status: Status,
    val error: Throwable? = null,
    data: T? = null
) {

    fun requireError(): Throwable = requireNotNull(error)

    private val _data: T? = data
    val data: T
        get() {
            return _data ?: throw RuntimeException(error)
        }
}

enum class Status {
    LOADING,
    SUCCESS,
    FAILED
}