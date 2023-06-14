package com.idunnololz.summit.util

import java.io.Serializable

class Result<out T> internal constructor(
    @PublishedApi
    internal val value: Any?
) : Serializable {
    // discovery

    /**
     * Returns `true` if this instance represents successful outcome.
     * In this case [isFailure] returns `false`.
     */
    public val isSuccess: Boolean get() = value !is Failure

    /**
     * Returns `true` if this instance represents failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    public val isFailure: Boolean get() = value is Failure

    // value & exception retrieval

    /**
     * Returns the encapsulated value if this instance represents [success][Result.isSuccess] or `null`
     * if it is [failure][Result.isFailure].
     *
     * This function is shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    public inline fun getOrNull(): T? =
        when {
            isFailure -> null
            else -> {
                @Suppress("UNCHECKED_CAST")
                value as T
            }
        }

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    /**
     * Returns a string `Success(v)` if this instance represents [success][Result.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    public override fun toString(): String =
        when (value) {
            is Failure -> value.toString() // "Failure($exception)"
            else -> "Success($value)"
        }

    // companion with constructors

    /**
     * Companion object for [Result] class that contains its constructor functions
     * [success] and [failure].
     */
    companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        fun <T> success(value: T): Result<T> =
            Result(value)

        /**
         * Returns an instance that encapsulates the given [exception] as failure.
         */
        fun <T> failure(exception: Throwable): Result<T> =
            Result(Failure(exception))
    }

    internal class Failure(
        val exception: Throwable
    ) : Serializable {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}