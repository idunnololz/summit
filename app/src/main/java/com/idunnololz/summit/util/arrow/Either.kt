@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)

package com.idunnololz.summit.util.arrow

import com.idunnololz.summit.util.arrow.Either.Left
import com.idunnololz.summit.util.arrow.Either.Right
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.ExperimentalExtendedContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * <!--- TEST_NAME EitherKnitTest -->
 *
 * In day-to-day programming, it is fairly common to find ourselves writing functions that can fail.
 * For instance, querying a service may result in a connection issue, or some unexpected JSON response.
 *
 * To communicate these errors, it has become common practice to throw exceptions; however,
 * exceptions are not tracked in any way, shape, or form by the compiler. To see what
 * kind of exceptions (if any) a function may throw, we have to dig through the source code.
 * Then, to handle these exceptions, we have to make sure we catch them at the call site. This
 * all becomes even more unwieldy when we try to compose exception-throwing procedures.
 *
 * ```kotlin
 * //sampleStart
 * val throwsSomeStuff: (Int) -> Double = {x -> x.toDouble()}
 * val throwsOtherThings: (Double) -> String = {x -> x.toString()}
 * val moreThrowing: (String) -> List<String> = {x -> listOf(x)}
 * val magic: (Int) -> List<String> = { x ->
 *   val y = throwsSomeStuff(x)
 *   val z = throwsOtherThings(y)
 *   moreThrowing(z)
 * }
 * //sampleEnd
 * fun main() {
 *  println ("magic = $magic")
 * }
 * ```
 * <!--- KNIT example-either-01.kt -->
 *
 * Assume we happily throw exceptions in our code. Looking at the types of the functions above, any could throw a number of exceptions -- we do not know. When we compose, exceptions from any of the constituent
 * functions can be thrown. Moreover, they may throw the same kind of exception
 * (e.g., `IllegalArgumentException`) and, thus, it gets tricky tracking exactly where an exception came from.
 *
 * How then do we communicate an error? By making it explicit in the data type we return.
 *
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 *
 * val left: Either<String, Int> =
 * //sampleStart
 *  Either.Left("Something went wrong")
 * //sampleEnd
 * fun main() {
 *  println(left)
 * }
 * ```
 * <!--- KNIT example-either-02.kt -->
 *
 * Because `Either` is right-biased, it is possible to define a Monad instance for it.
 *
 * Since we only ever want the computation to continue in the case of [Right] (as captured by the right-bias nature),
 * we fix the left type parameter and leave the right one free.
 *
 * So, the map and flatMap methods are right-biased:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * //sampleStart
 * val right: Either<String, Int> = Either.Right(5)
 * val value = right.flatMap{ Either.Right(it + 1) }
 * //sampleEnd
 * fun main() {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-either-03.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * //sampleStart
 * val left: Either<String, Int> = Either.Left("Something went wrong")
 * val value = left.flatMap{ Either.Right(it + 1) }
 * //sampleEnd
 * fun main() {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-either-04.kt -->
 *
 * ## Using Either instead of exceptions
 *
 * As a running example, we will have a series of functions that will:
 *
 * * Parse a string into an integer
 * * Calculate the reciprocal
 * * Convert the reciprocal into a string
 *
 * Using exception-throwing code, we could write something like this:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * //sampleStart
 * fun parse(s: String): Int =
 *   if (s.matches(Regex("-?[0-9]+"))) s.toInt()
 *   else throw NumberFormatException("$s is not a valid integer.")
 *
 * fun reciprocal(i: Int): Double =
 *   if (i == 0) throw IllegalArgumentException("Cannot take reciprocal of 0.")
 *   else 1.0 / i
 *
 * fun stringify(d: Double): String = d.toString()
 * //sampleEnd
 * ```
 * <!--- KNIT example-either-05.kt -->
 *
 * Instead, let's make the fact that some of our functions can fail explicit in the return type.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 * import com.idunnololz.summit.util.arrow.left
 * import com.idunnololz.summit.util.arrow.right
 *
 * //sampleStart
 * // Either Style
 * fun parse(s: String): Either<NumberFormatException, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(NumberFormatException("$s is not a valid integer."))
 *
 * fun reciprocal(i: Int): Either<IllegalArgumentException, Double> =
 *   if (i == 0) Either.Left(IllegalArgumentException("Cannot take reciprocal of 0."))
 *   else Either.Right(1.0 / i)
 *
 * fun stringify(d: Double): String = d.toString()
 *
 * fun magic(s: String): Either<Exception, String> =
 *   parse(s).flatMap { reciprocal(it) }.map { stringify(it) }
 * //sampleEnd
 * ```
 * <!--- KNIT example-either-06.kt -->
 *
 * These calls to `parse` return a [Left] and [Right] value
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 *
 * fun parse(s: String): Either<NumberFormatException, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(NumberFormatException("$s is not a valid integer."))
 *
 * //sampleStart
 * val notANumber = parse("Not a number")
 * val number2 = parse("2")
 * //sampleEnd
 * fun main() {
 *  println("notANumber = $notANumber")
 *  println("number2 = $number2")
 * }
 * ```
 * <!--- KNIT example-either-07.kt -->
 *
 * Now, using combinators like `flatMap` and `map`, we can compose our functions together.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * fun parse(s: String): Either<NumberFormatException, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(NumberFormatException("$s is not a valid integer."))
 *
 * fun reciprocal(i: Int): Either<IllegalArgumentException, Double> =
 *   if (i == 0) Either.Left(IllegalArgumentException("Cannot take reciprocal of 0."))
 *   else Either.Right(1.0 / i)
 *
 * fun stringify(d: Double): String = d.toString()
 *
 * fun magic(s: String): Either<Exception, String> =
 *   parse(s).flatMap{ reciprocal(it) }.map{ stringify(it) }
 *
 * //sampleStart
 * val magic0 = magic("0")
 * val magic1 = magic("1")
 * val magicNotANumber = magic("Not a number")
 * //sampleEnd
 * fun main() {
 *  println("magic0 = $magic0")
 *  println("magic1 = $magic1")
 *  println("magicNotANumber = $magicNotANumber")
 * }
 * ```
 * <!--- KNIT example-either-08.kt -->
 *
 * In the following exercise, we pattern-match on every case in which the `Either` returned by `magic` can be in.
 * Note the `when` clause in the [Left] - the compiler will complain if we leave that out because it knows that,
 * given the type `Either[Exception, String]`, there can be inhabitants of [Left] that are not
 * `NumberFormatException` or `IllegalArgumentException`. You should also notice that we are using
 * [SmartCast](https://kotlinlang.org/docs/reference/typecasts.html#smart-casts) for accessing [Left] and [Right]
 * values.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * fun parse(s: String): Either<NumberFormatException, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(NumberFormatException("$s is not a valid integer."))
 *
 * fun reciprocal(i: Int): Either<IllegalArgumentException, Double> =
 *   if (i == 0) Either.Left(IllegalArgumentException("Cannot take reciprocal of 0."))
 *   else Either.Right(1.0 / i)
 *
 * fun stringify(d: Double): String = d.toString()
 *
 * fun magic(s: String): Either<Exception, String> =
 *   parse(s).flatMap{ reciprocal(it) }.map{ stringify(it) }
 *
 * //sampleStart
 * val x = magic("2")
 * val value = when(x) {
 *   is Either.Left -> when (x.value) {
 *     is NumberFormatException -> "Not a number!"
 *     is IllegalArgumentException -> "Can't take reciprocal of 0!"
 *     else -> "Unknown error"
 *   }
 *   is Either.Right -> "Got reciprocal: ${x.value}"
 * }
 * //sampleEnd
 * fun main() {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-either-09.kt -->
 *
 * Instead of using exceptions as our error value, let's instead enumerate explicitly the things that
 * can go wrong in our program.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 * //sampleStart
 * // Either with ADT Style
 *
 * sealed class Error {
 *   object NotANumber : Error()
 *   object NoZeroReciprocal : Error()
 * }
 *
 * fun parse(s: String): Either<Error, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(Error.NotANumber)
 *
 * fun reciprocal(i: Int): Either<Error, Double> =
 *   if (i == 0) Either.Left(Error.NoZeroReciprocal)
 *   else Either.Right(1.0 / i)
 *
 * fun stringify(d: Double): String = d.toString()
 *
 * fun magic(s: String): Either<Error, String> =
 *   parse(s).flatMap{reciprocal(it)}.map{ stringify(it) }
 * //sampleEnd
 * ```
 * <!--- KNIT example-either-10.kt -->
 *
 * For our little module, we enumerate any and all errors that can occur. Then, instead of using
 * exception classes as error values, we use one of the enumerated cases. Now, when we pattern match,
 * we are able to comphrensively handle failure without resulting in an `else` branch; moreover,
 * since Error is sealed, no outside code can add additional subtypes that we might fail to handle.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.flatMap
 *
 * sealed class Error {
 *  object NotANumber : Error()
 *  object NoZeroReciprocal : Error()
 * }
 *
 * fun parse(s: String): Either<Error, Int> =
 *   if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
 *   else Either.Left(Error.NotANumber)
 *
 * fun reciprocal(i: Int): Either<Error, Double> =
 *   if (i == 0) Either.Left(Error.NoZeroReciprocal)
 *   else Either.Right(1.0 / i)
 *
 * fun stringify(d: Double): String = d.toString()
 *
 * fun magic(s: String): Either<Error, String> =
 *   parse(s).flatMap{ reciprocal(it) }.map{ stringify(it) }
 *
 * //sampleStart
 * val x = magic("2")
 * val value = when(x) {
 *   is Either.Left -> when (x.value) {
 *     is Error.NotANumber -> "Not a number!"
 *     is Error.NoZeroReciprocal -> "Can't take reciprocal of 0!"
 *   }
 *   is Either.Right -> "Got reciprocal: ${x.value}"
 * }
 * //sampleEnd
 * fun main() {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-either-11.kt -->
 *
 * ## Either.catch exceptions
 *
 * Sometimes you do need to interact with code that can potentially throw exceptions. In such cases, you should mitigate the possibility that an exception can be thrown. You can do so by using the `catch` function.
 *
 * Example:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 *
 * //sampleStart
 * fun potentialThrowingCode(): String = throw RuntimeException("Blow up!")
 *
 * suspend fun makeSureYourLogicDoesNotHaveSideEffects(): Either<Error, String> =
 *   Either.catch { potentialThrowingCode() }.mapLeft { Error.SpecificError }
 * //sampleEnd
 * suspend fun main() {
 *   println("makeSureYourLogicDoesNotHaveSideEffects().isLeft() = ${makeSureYourLogicDoesNotHaveSideEffects().isLeft()}")
 * }
 *
 * sealed class Error {
 *   object SpecificError : Error()
 * }
 * ```
 * <!--- KNIT example-either-12.kt -->
 *
 * ## Syntax
 *
 * Either can also map over the [Left] value with `mapLeft`, which is similar to map, but applies on left instances.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 *
 * //sampleStart
 * val r : Either<Int, Int> = Either.Right(7)
 * val rightMapLeft = r.mapLeft {it + 1}
 * val l: Either<Int, Int> = Either.Left(7)
 * val leftMapLeft = l.mapLeft {it + 1}
 * //sampleEnd
 * fun main() {
 *  println("rightMapLeft = $rightMapLeft")
 *  println("leftMapLeft = $leftMapLeft")
 * }
 * ```
 * <!--- KNIT example-either-13.kt -->
 *
 * `Either<A, B>` can be transformed to `Either<B,A>` using the `swap()` method.
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either.Left
 * import com.idunnololz.summit.util.arrow.Either
 *
 * //sampleStart
 * val r: Either<String, Int> = Either.Right(7)
 * val swapped = r.swap()
 * //sampleEnd
 * fun main() {
 *  println("swapped = $swapped")
 * }
 * ```
 * <!--- KNIT example-either-14.kt -->
 *
 * For using Either's syntax on arbitrary data types.
 * This will make possible to use the `left()`, `right()`, `getOrElse()` methods:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.right
 *
 * val right7 =
 * //sampleStart
 *   7.right()
 * //sampleEnd
 * fun main() {
 *  println(right7)
 * }
 * ```
 * <!--- KNIT example-either-15.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.left
 *
 *  val leftHello =
 * //sampleStart
 *  "hello".left()
 * //sampleEnd
 * fun main() {
 *  println(leftHello)
 * }
 * ```
 * <!--- KNIT example-either-16.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.left
 * import com.idunnololz.summit.util.arrow.getOrElse
 *
 * //sampleStart
 * val x = "hello".left()
 * val value = x.getOrElse { "$it world!" }
 * //sampleEnd
 * fun main() {
 *  println("value = $value")
 * }
 * ```
 * <!--- KNIT example-either-17.kt -->
 *
 * Another operation is `fold`. This operation will extract the value from the Either, or provide a default if the value is [Left]
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.right
 *
 * //sampleStart
 * val x : Either<Int, Int> = 7.right()
 * val fold = x.fold({ 1 }, { it + 3 })
 * //sampleEnd
 * fun main() {
 *  println("fold = $fold")
 * }
 * ```
 * <!--- KNIT example-either-18.kt -->
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.left
 *
 * //sampleStart
 * val y : Either<Int, Int> = 7.left()
 * val fold = y.fold({ 1 }, { it + 3 })
 * //sampleEnd
 * fun main() {
 *  println("fold = $fold")
 * }
 * ```
 * <!--- KNIT example-either-19.kt -->
 *
 * The `getOrElse()` operation allows the transformation of an `Either.Left` value to a `Either.Right` using
 * the value of [Left]. This can be useful when mapping to a single result type is required like `fold()`, but without
 * the need to handle `Either.Right` case.
 *
 * As an example, we want to map an `Either<Throwable, Int>` to a proper HTTP status code:
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.getOrElse
 *
 * //sampleStart
 * val r: Either<Throwable, Int> = Either.Left(NumberFormatException())
 * val httpStatusCode = r.getOrElse {
 *   when(it) {
 *     is NumberFormatException -> 400
 *     else -> 500
 *   }
 * }
 * //sampleEnd
 * fun main() {
 *  println("httpStatusCode = $httpStatusCode")
 * }
 * ```
 * <!--- KNIT example-either-20.kt -->
 */
sealed class Either<out A, out B> {

  /**
   * Returns `true` if this is a [Left], `false` otherwise.
   */
  fun isLeft(): Boolean {
    contract {
      returns(true) implies (this@Either is Left)
      returns(false) implies (this@Either is Right)
    }
    return this@Either is Left<A>
  }

  /**
   * Returns `true` if this is a [Right], `false` otherwise.
   */
  fun isRight(): Boolean {
    contract {
      returns(true) implies (this@Either is Right)
      returns(false) implies (this@Either is Left)
    }
    return this@Either is Right<B>
  }

  /**
   * Returns `false` if [Right]
   * or returns the result of the given [predicate] to the [Left] value.
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import com.idunnololz.summit.util.arrow.Either.Left
   * import com.idunnololz.summit.util.arrow.Either.Right
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *  Left(12).isLeft { it > 10 } shouldBe true
   *  Left(7).isLeft { it > 10 } shouldBe false
   *
   *  val right: Either<Int, String> = Right("Hello World")
   *  right.isLeft { it > 10 } shouldBe false
   * }
   * ```
   * <!--- KNIT example-either-21.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun isLeft(predicate: (A) -> Boolean): Boolean {
    contract {
      returns(true) implies (this@Either is Left)
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
      (this@Either is Left) holdsIn predicate
    }
    return this@Either is Left<A> && predicate(value)
  }

  /**
   * Returns `false` if [Left]
   * or returns the result of the given [predicate] to the [Right] value.
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import com.idunnololz.summit.util.arrow.Either.Left
   * import com.idunnololz.summit.util.arrow.Either.Right
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *  Right(12).isRight { it > 10 } shouldBe true
   *  Right(7).isRight { it > 10 } shouldBe false
   *
   *  val left: Either<String, Int> = Left("Hello World")
   *  left.isRight { it > 10 } shouldBe false
   * }
   * ```
   * <!--- KNIT example-either-22.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun isRight(predicate: (B) -> Boolean): Boolean {
    contract {
      returns(true) implies (this@Either is Right)
      callsInPlace(predicate, InvocationKind.AT_MOST_ONCE)
      (this@Either is Right) holdsIn predicate
    }
    return this@Either is Right<B> && predicate(value)
  }

  /**
   * Transform an [Either] into a value of [C].
   * Alternative to using `when` to fold an [Either] into a value [C].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   * import io.kotest.assertions.AssertionErrorBuilder.Companion.fail
   *
   * fun test() {
   *   Either.Right(1)
   *     .fold({ fail("Cannot be left") }, { it + 1 }) shouldBe 2
   *
   *   Either.Left(RuntimeException("Boom!"))
   *     .fold({ -1 }, { fail("Cannot be right") }) shouldBe -1
   * }
   * ```
   * <!--- KNIT example-either-23.kt -->
   * <!--- TEST lines.isEmpty() -->
   *
   * @param ifLeft transform the [Either.Left] type [A] to [C].
   * @param ifRight transform the [Either.Right] type [B] to [C].
   * @return the transformed value [C] by applying [ifLeft] or [ifRight] to [A] or [B] respectively.
   */
  inline fun <C> fold(ifLeft: (left: A) -> C, ifRight: (right: B) -> C): C {
    contract {
      callsInPlace(ifLeft, InvocationKind.AT_MOST_ONCE)
      callsInPlace(ifRight, InvocationKind.AT_MOST_ONCE)
      (this@Either is Left) holdsIn ifLeft
      (this@Either is Right) holdsIn ifRight
    }
    return when (this) {
      is Right -> ifRight(value)
      is Left -> ifLeft(value)
    }
  }

  /**
   * Swap the generic parameters [A] and [B] of this [Either].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Left("left").swap() shouldBe Either.Right("left")
   *   Either.Right("right").swap() shouldBe Either.Left("right")
   * }
   * ```
   * <!--- KNIT example-either-24.kt -->
   * <!-- TEST lines.isEmpty() -->
   */
  fun swap(): Either<B, A> =
    fold({ Right(it) }, { Left(it) })

  /**
   * Map, or transform, the right value [B] of this [Either] to a new value [C].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Right(12).map { _: Int ->"flower" } shouldBe Either.Right("flower")
   *   Either.Left(12).map { _: Nothing -> "flower" } shouldBe Either.Left(12)
   * }
   * ```
   * <!--- KNIT example-either-25.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun <C> map(f: (right: B) -> C): Either<A, C> {
    contract {
      callsInPlace(f, InvocationKind.AT_MOST_ONCE)
      (this@Either is Right) holdsIn f
    }
    return flatMap { Right(f(it)) }
  }


  /**
   * Map, or transform, the left value [A] of this [Either] to a new value [C].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *  Either.Right(12).mapLeft { _: Nothing -> "flower" } shouldBe Either.Right(12)
   *  Either.Left(12).mapLeft { _: Int -> "flower" }  shouldBe Either.Left("flower")
   * }
   * ```
   * <!--- KNIT example-either-26.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun <C> mapLeft(f: (A) -> C): Either<C, B> {
    contract {
      callsInPlace(f, InvocationKind.AT_MOST_ONCE)
      (this@Either is Left) holdsIn f
    }
    return when (this) {
      is Left -> Left(f(value))
      is Right -> Right(value)
    }
  }

  /**
   * Performs the given [action] on the encapsulated [B] value if this instance represents [Either.Right].
   * Returns the original [Either] unchanged.
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Right(1).onRight(::println) shouldBe Either.Right(1)
   * }
   * ```
   * <!--- KNIT example-either-27.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun onRight(action: (right: B) -> Unit): Either<A, B> {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
      (this@Either is Right) holdsIn action
    }
    return also { if (it.isRight()) action(it.value) }
  }

  /**
   * Performs the given [action] on the encapsulated [A] if this instance represents [Either.Left].
   * Returns the original [Either] unchanged.
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Left(2).onLeft(::println) shouldBe Either.Left(2)
   * }
   * ```
   * <!--- KNIT example-either-28.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  inline fun onLeft(action: (left: A) -> Unit): Either<A, B> {
    contract {
      callsInPlace(action, InvocationKind.AT_MOST_ONCE)
      (this@Either is Left) holdsIn action
    }
    return also { if (it.isLeft()) action(it.value) }
  }

  /**
   * Returns the unwrapped value [B] of [Either.Right] or `null` if it is [Either.Left].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Right(12).getOrNull() shouldBe 12
   *   Either.Left(12).getOrNull() shouldBe null
   * }
   * ```
   * <!--- KNIT example-either-29.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  fun getOrNull(): B? {
    contract {
      returnsNotNull() implies (this@Either is Right)
    }
    return getOrElse { null }
  }

  /**
   * Returns the unwrapped value [A] of [Either.Left] or `null` if it is [Either.Right].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Right(12).leftOrNull() shouldBe null
   *   Either.Left(12).leftOrNull() shouldBe 12
   * }
   * ```
   * <!--- KNIT example-either-30.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  fun leftOrNull(): A? {
    contract {
      returnsNotNull() implies (this@Either is Left)
    }
    return fold(::identity) { null }
  }

  /**
   * Transforms [Either] into [Option],
   * where the encapsulated value [B] is wrapped in [Some] when this instance represents [Either.Right],
   * or [None] if it is [Either.Left].
   *
   * ```kotlin
   * import com.idunnololz.summit.util.arrow.Either
   * import com.idunnololz.summit.util.arrow.Some
   * import com.idunnololz.summit.util.arrow.None
   * import io.kotest.matchers.shouldBe
   *
   * fun test() {
   *   Either.Right(12).getOrNone() shouldBe Some(12)
   *   Either.Left(12).getOrNone() shouldBe None
   * }
   * ```
   * <!--- KNIT example-either-31.kt -->
   * <!--- TEST lines.isEmpty() -->
   */
  fun getOrNone(): Option<B> = fold({ None }, { Some(it) })


  /**
   * The left side of the disjoint union, as opposed to the [Right] side.
   */
  data class Left<out A> constructor(val value: A) : Either<A, Nothing>() {
    override fun toString(): String = "Either.Left($value)"

    companion object
  }

  /**
   * The right side of the disjoint union, as opposed to the [Left] side.
   */
  data class Right<out B> constructor(val value: B) : Either<Nothing, B>() {
    override fun toString(): String = "Either.Right($value)"

    companion object {
      @PublishedApi
      internal val unit: Either<Nothing, Unit> = Right(Unit)
    }
  }

  override fun toString(): String = fold(
    { "Either.Left($it)" },
    { "Either.Right($it)" }
  )
}

/**
 * Binds the given function across [Right], that is,
 * Map, or transform, the right value [B] of this [Either] into a new [Either] with a right value of type [C].
 * Returns a new [Either] with either the original left value of type [A] or the newly transformed right value of type [C].
 *
 * @param f The function to bind across [Right].
 */
inline fun <A, B, C> Either<A, B>.flatMap(f: (right: B) -> Either<A, C>): Either<A, C> {
  contract {
    callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    (this@flatMap is Right) holdsIn f
  }
  return when (this) {
    is Right -> f(this.value)
    is Left -> this
  }
}

/**
 * Binds the given function across [Left], that is,
 * Map, or transform, the left value [A] of this [Either] into a new [Either] with a left value of type [C].
 * Returns a new [Either] with either the original right value of type [B] or the newly transformed left value of type [C].
 *
 * @param f The function to bind across [Left].
 */
inline fun <A, B, C> Either<A, B>.handleErrorWith(f: (A) -> Either<C, B>): Either<C, B> {
  contract {
    callsInPlace(f, InvocationKind.AT_MOST_ONCE)
    (this@handleErrorWith is Left) holdsIn f
  }
  return when (this) {
    is Left -> f(this.value)
    is Right -> this
  }
}

fun <A, B> Either<A, Either<A, B>>.flatten(): Either<A, B> =
  flatMap(::identity)

/**
 * Get the right value [B] of this [Either],
 * or compute a [default] value with the left value [A].
 *
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either
 * import com.idunnololz.summit.util.arrow.getOrElse
 * import io.kotest.matchers.shouldBe
 *
 * fun test() {
 *   Either.Left(12) getOrElse { it + 5 } shouldBe 17
 * }
 * ```
 * <!--- KNIT example-either-33.kt -->
 * <!--- TEST lines.isEmpty() -->
 */
inline infix fun <A, B> Either<A, B>.getOrElse(default: (A) -> B): B {
  contract {
    callsInPlace(default, InvocationKind.AT_MOST_ONCE)
    (this@getOrElse is Left) holdsIn default
  }
  return when (this) {
    is Left -> default(this.value)
    is Right -> this.value
  }
}

/**
 * Returns the value from this [Right] or [Left].
 *
 * Example:
 * ```kotlin
 * import com.idunnololz.summit.util.arrow.Either.Left
 * import com.idunnololz.summit.util.arrow.Either.Right
 * import com.idunnololz.summit.util.arrow.merge
 *
 * fun test() {
 *   Right(12).merge() // Result: 12
 *   Left(12).merge() // Result: 12
 * }
 * ```
 * <!--- KNIT example-either-34.kt -->
 * <!--- TEST lines.isEmpty() -->
 */
fun <A> Either<A, A>.merge(): A =
  fold(::identity, ::identity)

fun <A> A.left(): Either<A, Nothing> = Left(this)

fun <A> A.right(): Either<Nothing, A> = Right(this)

operator fun <A : Comparable<A>, B : Comparable<B>> Either<A, B>.compareTo(other: Either<A, B>): Int =
  fold(
    { a1 -> other.fold({ a2 -> a1.compareTo(a2) }, { -1 }) },
    { b1 -> other.fold({ 1 }, { b2 -> b1.compareTo(b2) }) }
  )

/**
 * Combine two [Either] values.
 * If both are [Right] then combine both [B] values using [combineRight] or if both are [Left] then combine both [A] values using [combineLeft],
 * otherwise return the sole [Left] value (either `this` or [other]).
 */
inline fun <A, B> Either<A, B>.combine(other: Either<A, B>, combineLeft: (A, A) -> A, combineRight: (B, B) -> B): Either<A, B> {
  contract {
    callsInPlace(combineLeft, InvocationKind.AT_MOST_ONCE)
    callsInPlace(combineRight, InvocationKind.AT_MOST_ONCE)
    (this@combine is Left && other is Left) holdsIn combineLeft
    (this@combine is Right && other is Right) holdsIn combineRight
  }
  return when (val one = this) {
    is Left -> when (other) {
      is Left -> Left(combineLeft(one.value, other.value))
      is Right -> one
    }

    is Right -> when (other) {
      is Left -> other
      is Right -> Right(combineRight(one.value, other.value))
    }
  }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <A> identity(a: A): A = a