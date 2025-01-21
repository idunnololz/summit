package com.idunnololz.summit.util

import android.content.SharedPreferences
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimationsHelper @Inject constructor(
    private val preferences: Preferences,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {
    enum class AnimationLevel(val animationLevel: Int) {
        /**
         * Animations that are absolutely critical.
         */
        Critical(0),

        /**
         * Animations dealing with status, nav bar or tool bar
         */
        Navigation(3),

        /**
         * Small animations that app polish to the app. Eg. fade animations.
         */
        Polish(4),

        /**
         * Things like shared element transition animations
         */
        Extras(10),
        Max(11),
        ;

        companion object {
            fun parse(level: Int) = when (level) {
                0, 1, 2 -> Critical
                3 -> Navigation
                4, 5, 6, 7, 8, 9 -> Polish
                10 -> Extras
                11 -> Max
                else -> Max
            }
        }
    }
    private val coroutineScope = coroutineScopeFactory.create()

    private var animationLevel: AnimationLevel = preferences.animationLevel

    init {
        coroutineScope.launch(Dispatchers.Main) {
            animationLevel = preferences.animationLevel
        }
    }

    fun shouldAnimate(animationLevel: AnimationLevel) =
        this.animationLevel.animationLevel >= animationLevel.animationLevel

    fun doAnimation(animationLevel: AnimationLevel, fn: () -> Unit) {
        if (shouldAnimate(animationLevel)) {
            fn()
        }
    }
}
