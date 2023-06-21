package com.idunnololz.summit.util

import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation

object AnimationUtils {

    private const val DEFAULT_ANIMATION_DURATION_MS: Long = 300

    const val IMAGE_LOAD_CROSS_FADE_DURATION_MS: Long = 200

    fun makeAnimationControllerFor(view: View): AnimationController = AnimationController(view)

    fun animateInView(view: View, duration: Long, startDelay: Long, force: Boolean) {
        view.animate().cancel()
        view.clearAnimation()

        if (force) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
        }

        if (view.visibility == View.VISIBLE && view.alpha == 1f) {
            return
        }
        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
        }

        view.translationY = Utils.convertDpToPixel(40f)
        ViewCompat.animate(view)
            .setDuration(duration)
            .setStartDelay(startDelay)
            .translationY(0f)
            .alpha(1f)
            .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationCancel(view: View) {
                    view?.translationY = 0f
                }
            })
    }

    fun animateInInnerViewsWithDelay(rootView: View, toIgnore: Set<View>, duration: Long) {
        animateInInnerViewsWithDelay(rootView, 0L, toIgnore, duration)
    }

    private fun animateInInnerViewsWithDelay(
        rootView: View,
        delay: Long,
        toIgnore: Set<View>,
        duration: Long
    ) {
        if (rootView is ViewGroup) {
            val childCount = rootView.childCount
            for (i in 0 until childCount) {
                val v = rootView.getChildAt(i)
                animateInInnerViewsWithDelay(v, delay, toIgnore, duration)
            }
        } else {
            if (!toIgnore.contains(rootView)) {
                animateInView(rootView, duration, delay, true)
            }
        }
    }

    @JvmOverloads
    fun expand(v: View, duration: Long = DEFAULT_ANIMATION_DURATION_MS) {
        if (v.visibility == View.VISIBLE
            && v.alpha != 0f
            && v.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT
        ) {
            return
        }

        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) destroy animations for views with a height of 0.
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1f)
                    ViewGroup.LayoutParams.WRAP_CONTENT
                else
                    (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = duration
        v.startAnimation(a)
    }

    fun collapse(v: View, endVisibility: Int) {
        val initialHeight = v.measuredHeight

        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = endVisibility
                    v.layoutParams.height = 0
                    v.requestLayout()
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = 300
        v.startAnimation(a)
        v.requestLayout()
    }

    class AnimationController(private val view: View) {
        private var animatingIn: Boolean = false
        private var animatingOut: Boolean = false

        var hideVisibility = View.INVISIBLE

        val isVisible: Boolean
            get() = view.visibility == View.VISIBLE && view.alpha != 0f

        @JvmOverloads
        fun show(animate: Boolean = true, duration: Int = -1) {
            if (animate) {
                if (animatingIn) {
                    return
                }

                animatingIn = true
                animatingOut = false

                view.animate().cancel()
                view.clearAnimation()

                if (view.visibility == View.VISIBLE && view.alpha == 1f) {
                    return
                }
                if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.alpha = 0f
                }

                val animator = ViewCompat
                    .animate(view)
                    .alpha(1f)
                    .withStartAction { }
                if (duration > 0) {
                    animator.duration = duration.toLong()
                }
            } else {
                animatingIn = false
                animatingOut = false

                view.visibility = View.VISIBLE
                view.alpha = 1f
            }
        }

        @JvmOverloads
        fun hide(animate: Boolean = true, duration: Int = -1) {
            if (animate) {
                if (animatingOut) {
                    return
                }

                animatingOut = true
                animatingIn = false

                view.animate().cancel()
                view.clearAnimation()

                if (view.visibility == hideVisibility) {
                    return
                }
                if (view.visibility != View.VISIBLE) {
                    view.visibility = hideVisibility
                    return
                }

                val animator = ViewCompat
                    .animate(view)
                    .alpha(0f)
                    .withEndAction { view.visibility = hideVisibility }
                if (duration > 0) {
                    animator.duration = duration.toLong()
                }
            } else {
                animatingIn = false
                animatingOut = false

                view.visibility = hideVisibility
            }
        }

        fun cancelAnimations() {
            view.animate().cancel()
            view.clearAnimation()
        }
    }
}
