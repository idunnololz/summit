@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)

package com.idunnololz.summit.util.arrow

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmStatic

/**
 * <!--- TEST_NAME OptionKnitTest -->
 *
 * If you have worked with Java at all in the past, it is very likely that you have come across a `NullPointerException` at some time (other languages will throw similarly named errors in such a case). Usually this happens because some method returns `null` when you weren't expecting it and, thus, isn't dealing with that possibility in your client code. A value of `null` is often abused to represent an absent optional value.
 * Kotlin tries to solve the problem by getting rid of `null` values altogether, and providing its own special syntax [Null-safety machinery based on `?`](https://kotlinlang.org/docs/reference/null-safety.html).
 *
 * Arrow models the absence of values through the `Option` datatype similar to how Scala, Haskell, and other FP languages handle optional values.
 *
 * `Option<A>` is a container for an optional value of type `A`. If the value of type `A` is present, the `Option<A>` is an instance of `Some<A>`, containing the present value of type `A`. If the value is absent, the `Option<A>` is the object `None`.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 * import com.idunnololz.summit.util.arrow.none
 *
 * //sampleStart
 * val someValue: Option<String> = Some("I am wrapped in something")
 * val emptyValue: Option<String> = none()
 * //sampleEnd
 * fun main() {
 *  println("value = $someValue")
 *  println("emptyValue = $emptyValue")
 * }
 * ```
 * <!--- KNIT example-option-01.kt -->
 *
 * Let's write a function that may or may not give us a string, thus returning `Option<String>`:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * //sampleStart
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 * //sampleEnd
 * ```
 * <!--- KNIT example-option-02.kt -->
 *
 * Using `getOrElse`, we can provide a default value `"No value"` when the optional argument `None` does not exist:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 * import com.idunnololz.summit.util.arrow.getOrElse
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 * val value1 =
 * //sampleStart
 *  maybeItWillReturnSomething(true)
 *     .getOrElse { "No value" }
 * //sampleEnd
 * fun main() {
 *  println(value1)
 * }
 * ```
 * <!--- KNIT example-option-03.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 * import com.idunnololz.summit.util.arrow.getOrElse
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 * val value2 =
 * //sampleStart
 *  maybeItWillReturnSomething(false)
 *   .getOrElse { "No value" }
 * //sampleEnd
 * fun main() {
 *  println(value2)
 * }
 * ```
 * <!--- KNIT example-option-04.kt -->
 *
 * Checking whether option has value:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
 *  if (flag) Some("Found value") else None
 *
 *  //sampleStart
 * val valueSome = maybeItWillReturnSomething(true) is None
 * val valueNone = maybeItWillReturnSomething(false) is None
 * //sampleEnd
 * fun main() {
 *  println("valueSome = $valueSome")
 *  println("valueNone = $valueNone")
 * }
 * ```
 * <!--- KNIT example-option-05.kt -->
 * Creating a `Option<T>` of a `T?`. Useful for working with values that can be nullable:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Option
 *
 * //sampleStart
 * val myString: String? = "Nullable string"
 * val option: Option<String> = Option.fromNullable(myString)
 * //sampleEnd
 * fun main () {
 *  println("option = $option")
 * }
 * ```
 * <!--- KNIT example-option-06.kt -->
 *
 * Option can also be used with when statements:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * //sampleStart
 * val someValue: Option<Double> = Some(20.0)
 * val value = when(someValue) {
 *  is Some -> someValue.value
 *  is None -> 0.0
 * }
 * //sampleEnd
 * fun main () {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-option-07.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * //sampleStart
 * val noValue: Option<Double> = None
 * val value = when(noValue) {
 *  is Some -> noValue.value
 *  is None -> 0.0
 * }
 * //sampleEnd
 * fun main () {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-option-08.kt -->
 *
 * An alternative for pattern matching is folding. This is possible because an option could be looked at as a collection or foldable structure with either one or zero elements.
 *
 * One of these operations is `map`. This operation allows us to map the inner value to a different type while preserving the option:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.None
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * //sampleStart
 * val number: Option<Int> = Some(3)
 * val noNumber: Option<Int> = None
 * val mappedResult1 = number.map { it * 1.5 }
 * val mappedResult2 = noNumber.map { it * 1.5 }
 * //sampleEnd
 * fun main () {
 *  println("number = $number")
 *  println("noNumber = $noNumber")
 *  println("mappedResult1 = $mappedResult1")
 *  println("mappedResult2 = $mappedResult2")
 * }
 * ```
 * <!--- KNIT example-option-09.kt -->
 * Another operation is `fold`. This operation will extract the value from the option, or provide a default if the value is `None`
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.Some
 *
 * val fold =
 * //sampleStart
 *  Some(3).fold({ 1 }, { it * 3 })
 * //sampleEnd
 * fun main () {
 *  println(fold)
 * }
 * ```
 * <!--- KNIT example-option-10.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Option
 * import com.idunnololz.summit.util.arrow.none
 *
 * val fold =
 * //sampleStart
 *  none<Int>().fold({ 1 }, { it * 3 })
 * //sampleEnd
 * fun main () {
 *  println(fold)
 * }
 * ```
 * <!--- KNIT example-option-11.kt -->
 *
 * Arrow also adds syntax to all datatypes so you can easily lift them into the context of `Option` where needed.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.some
 * import com.idunnololz.summit.util.arrow.none
 *
 * //sampleStart
 *  val some = 1.some()
 *  val none = none<String>()
 * //sampleEnd
 * fun main () {
 *  println("some = $some")
 *  println("none = $none")
 * }
 * ```
 * <!--- KNIT example-option-12.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.toOption
 *
 * //sampleStart
 * val nullString: String? = null
 * val valueFromNull = nullString.toOption()
 *
 * val helloString: String? = "Hello"
 * val valueFromStr = helloString.toOption()
 * //sampleEnd
 * fun main () {
 *  println("valueFromNull = $valueFromNull")
 *  println("valueFromStr = $valueFromStr")
 * }
 * ```
 * <!--- KNIT example-option-13.kt -->
 *
 * You can easily convert between `A?` and `Option<A>` by using the `toOption()` extension or `Option.fromNullable` constructor.
 *
 * ```kotlin
 * import arrow.core.firstOrNone
 * import com.idunnololz.summit.util.arrow.toOption
 * import com.idunnololz.summit.util.arrow.Option
 *
 * //sampleStart
 * val foxMap = mapOf(1 to "The", 2 to "Quick", 3 to "Brown", 4 to "Fox")
 *
 * val empty = foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() }.toOption()
 * val filled = Option.fromNullable(foxMap.entries.firstOrNull { it.key == 5 }?.value.let { it?.toCharArray() })
 *
 * //sampleEnd
 * fun main() {
 *  println("empty = $empty")
 *  println("filled = $filled")
 * }
 * ```
 * <!--- KNIT example-option-14.kt -->
 *
 * ### Transforming the inner contents
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Some
 *
 * fun main() {
 * val value =
 *  //sampleStart
 *    Some(1).map { it + 1 }
 *  //sampleEnd
 *  println(value)
 * }
 * ```
 * <!--- KNIT example-option-15.kt -->
 *
 * ## Credits
 *
 * Contents partially adapted from [Scala Exercises Option Tutorial](https://www.scala-exercises.org/std_lib/options)
 * Originally based on the Scala Koans.
 */
sealed class Option<out A> {

  companion object {

    @JvmStatic
    fun <A> fromNullable(a: A?): Option<A> = if (a != null) Some(a) else None

    @JvmStatic
    operator fun <A> invoke(a: A): Option<A> = Some(a)
  }

  /**
   * The given function is applied as a fire and forget effect
   * if this is a `None`.
   * When applied the result is ignored and the original
   * None value is returned
   *
   * Example:
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Some
   * import com.idunnololz.summit.util.arrow.none
   *
   * fun main() {
   *   Some(12).onNone { println("flower") } // Result: Some(12)
   *   none<Int>().onNone { println("flower") }  // Result: prints "flower" and returns: None
   * }
   * ```
   * <!--- KNIT example-option-16.kt -->
   */
  inline fun onNone(action: () -> Unit): Option<A>  {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
      (this@Option is None) holdsIn action
    }
    return also { if (it.isNone()) action() }
  }

  /**
   * The given function is applied as a fire and forget effect
   * if this is a `some`.
   * When applied the result is ignored and the original
   * Some value is returned
   *
   * Example:
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Some
   * import com.idunnololz.summit.util.arrow.none
   *
   * fun main() {
   *   Some(12).onSome { println("flower") } // Result: prints "flower" and returns: Some(12)
   *   none<Int>().onSome { println("flower") }  // Result: None
   * }
   * ```
   * <!--- KNIT example-option-17.kt -->
   */
  inline fun onSome(action: (A) -> Unit): Option<A>  {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
      (this@Option is Some) holdsIn action
    }
    return also { if (it.isSome()) action(it.value) }
  }

  /**
   * Returns true if the option is [None], false otherwise.
   * @note Used only for performance instead of fold.
   */
  fun isNone(): Boolean {
    contract {
      returns(false) implies (this@Option is Some)
      returns(true) implies (this@Option is None)
    }
    return this@Option is None
  }

  /**
   * Returns true if the option is [Some], false otherwise.
   * @note Used only for performance instead of fold.
   */
  fun isSome(): Boolean {
    contract {
      returns(true) implies (this@Option is Some)
      returns(false) implies (this@Option is None)
    }
    return this@Option is Some<A>
  }

  /**
   * Returns true if this option is nonempty '''and''' the predicate
   * $p returns true when applied to this $option's value.
   * Otherwise, returns false.
   *
   * Example:
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Some
   * import com.idunnololz.summit.util.arrow.None
   * import com.idunnololz.summit.util.arrow.Option
   *
   * fun main() {
   *   Some(12).isSome { it > 10 } // Result: true
   *   Some(7).isSome { it > 10 }  // Result: false
   *
   *   val none: Option<Int> = None
   *   none.isSome { it > 10 }      // Result: false
   * }
   * ```
   * <!--- KNIT example-option-18.kt -->
   *
   * @param predicate the predicate to test
   */
  inline fun isSome(predicate: (A) -> Boolean): Boolean {
    contract {
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
      returns(true) implies (this@Option is Some)
      (this@Option is Some) holdsIn predicate
    }
    return this@Option is Some<A> && predicate(value)
  }

  /**
   * Returns the encapsulated value [A] if this instance represents [Some] or `null` if it is [None].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.None
   * import com.idunnololz.summit.util.arrow.Some
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Some(12).getOrNull() shouldBe 12
   *   None.getOrNull() shouldBe null
   * }
   * ```
   * <!--- KNIT example-option-19.kt -->
   */
  fun getOrNull(): A? {
    contract {
      returnsNotNull() implies (this@Option is Some)
    }
    return getOrElse { null }
  }

  /**
   * Returns a [Some<$B>] containing the result of applying $f to this $option's
   * value if this $option is nonempty. Otherwise return $none.
   *
   * @note This is similar to `flatMap` except here,
   * $f does not need to wrap its result in an $option.
   *
   * @param f the function to apply
   * @see flatMap
   */
  inline fun <B> map(f: (A) -> B): Option<B> {
    contract {
      callsInPlace(f, InvocationKind.AT_MOST_ONCE)
      (this@Option is Some) holdsIn f
    }
    return flatMap { a -> Some(f(a)) }
  }

  inline fun <R> fold(ifEmpty: () -> R, ifSome: (A) -> R): R {
    contract {
      callsInPlace(ifEmpty, InvocationKind.AT_MOST_ONCE)
      callsInPlace(ifSome, InvocationKind.AT_MOST_ONCE)
      (this@Option is None) holdsIn ifEmpty
      (this@Option is Some) holdsIn ifSome
    }
    return when (this) {
      is None -> ifEmpty()
      is Some<A> -> ifSome(value)
    }
  }

  /**
   * Returns the result of applying $f to this $option's value if
   * this $option is nonempty.
   * Returns $none if this $option is empty.
   * Slightly different from `map` in that $f is expected to
   * return an $option (which could be $none).
   *
   * @param f the function to apply
   * @see map
   */
  inline fun <B> flatMap(f: (A) -> Option<B>): Option<B> {
    contract {
      callsInPlace(f, InvocationKind.AT_MOST_ONCE)
      (this@Option is Some) holdsIn f
    }
    return when (this) {
      is None -> this
      is Some -> f(value)
    }
  }

  /**
   * Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns true. Otherwise, return $none.
   *
   *  @param predicate the predicate used for testing.
   */
  inline fun filter(predicate: (A) -> Boolean): Option<A> {
    contract {
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
      (this@Option is Some) holdsIn predicate
    }
    return flatMap { a -> if (predicate(a)) Some(a) else None }
  }

  /**
   * Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns false. Otherwise, return $none.
   *
   * @param predicate the predicate used for testing.
   */
  inline fun filterNot(predicate: (A) -> Boolean): Option<A> {
    contract {
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
      (this@Option is Some) holdsIn predicate
    }
    return flatMap { a -> if (!predicate(a)) Some(a) else None }
  }

  inline fun <L> toEither(ifEmpty: () -> L): Either<L, A> {
    contract {
      callsInPlace(ifEmpty, InvocationKind.AT_MOST_ONCE)
      (this@Option is None) holdsIn ifEmpty
    }
    return fold({ ifEmpty().left() }, { it.right() })
  }

  fun toList(): List<A> = fold(::emptyList) { listOf(it) }

  override fun toString(): String = fold(
    { "Option.None" },
    { "Option.Some($it)" }
  )
}

object None : Option<Nothing>() {
  override fun toString(): String = "Option.None"
}

data class Some<out T>(val value: T) : Option<T>() {
  override fun toString(): String = "Option.Some($value)"

  companion object
}

/**
 * Returns the option's value if the option is nonempty, otherwise
 * return the result of evaluating `default`.
 *
 * @param default the default expression.
 */
inline fun <T> Option<T>.getOrElse(default: () -> T): T {
  contract {
    callsInPlace(default, InvocationKind.AT_MOST_ONCE)
    (this@getOrElse is None) holdsIn default
  }
  return when (this) {
    is Some -> value
    else -> default()
  }
}

fun <T> T?.toOption(): Option<T> = this?.let { Some(it) } ?: None

fun <A> A.some(): Option<A> = Some(this)

fun <A> none(): Option<A> = None

/**
 * Returns an Option containing all elements that are instances of specified type parameter [B].
 */
inline fun <reified B> Option<*>.filterIsInstance(): Option<B> =
  flatMap {
    when (it) {
      is B -> Some(it)
      else -> None
    }
  }

fun <A> Option<Option<A>>.flatten(): Option<A> =
  flatMap(::identity)

fun <K, V> Option<Pair<K, V>>.toMap(): Map<K, V> = this.toList().toMap()

inline fun <A> Option<A>.combine(other: Option<A>, combine: (A, A) -> A): Option<A> {
  contract {
    callsInPlace(combine, InvocationKind.AT_MOST_ONCE)
    (this@combine is Some && other is Some) holdsIn combine
  }
  return when (this) {
    is Some -> when (other) {
      is Some -> Some(combine(value, other.value))
      None -> this
    }

    None -> other
  }
}

operator fun <A : Comparable<A>> Option<A>.compareTo(other: Option<A>): Int = fold(
  { other.fold({ 0 }, { -1 }) },
  { a1 ->
    other.fold({ 1 }, { a2 -> a1.compareTo(a2) })
  }
)