package com.idunnololz.summit.lemmy.utils.listSource

sealed interface PageResult<T> {
  val pageIndex: Int

  data class SuccessPageResult<T>(
    override val pageIndex: Int,
    val items: List<T>,
    val hasMore: Boolean,
  ) : PageResult<T>

  data class ErrorPageResult<T>(
    override val pageIndex: Int,
    val error: Throwable,
  ) : PageResult<T>

  fun getOrNull() =
    this as? SuccessPageResult<T>

  fun exceptionOrNull() =
    (this as? ErrorPageResult<T>)?.error

  fun <R> fold(
    onSuccess: (value: SuccessPageResult<T>) -> R,
    onFailure: (exception: Throwable) -> R
  ): R {
    return when (val exception = exceptionOrNull()) {
      null -> onSuccess(requireNotNull(getOrNull()))
      else -> onFailure(exception)
    }
  }
}

inline fun <T> PageResult<T>.onFailure(action: (exception: Throwable) -> Unit): PageResult<T> {
  exceptionOrNull()?.let { action(it) }
  return this
}

inline fun <T> PageResult<T>.onSuccess(action: (value: PageResult.SuccessPageResult<T>) -> Unit): PageResult<T> {
  getOrNull()?.let { action(it) }
  return this
}