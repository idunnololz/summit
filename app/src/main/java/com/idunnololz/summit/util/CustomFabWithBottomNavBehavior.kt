package com.idunnololz.summit.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R

@SuppressLint("RestrictedApi")
class CustomFabWithBottomNavBehavior(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    private var bottomNavHeight: Float = 0f
    private var snackbarHeight: Float = 0f
    private var bottomInset: Int = 0
    private val snackBarBottomMargin = context.resources.getDimensionPixelSize(R.dimen.padding)

    fun updateBottomNavHeight(height: Float) {
        Log.d("HAHA", "updateBottomNavHeight: $height")
        bottomNavHeight = height
    }

    fun updateBottomInset(bottomInset: Int) {
        Log.d("HAHA", "bottomInset: $bottomInset")
        this.bottomInset = bottomInset
    }

    override fun getInsetDodgeRect(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        rect: Rect,
    ): Boolean {
        return super.getInsetDodgeRect(parent, child, rect)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View,
    ): Boolean {
        if (dependency is Snackbar.SnackbarLayout) {
            updateSnackbar(child, dependency)
        }
        return dependency is AppBarLayout || dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View,
    ): Boolean {
        if (dependency is Snackbar.SnackbarLayout) {
            snackbarHeight = dependency.height.toFloat() + snackBarBottomMargin
        }

        return updateFab(child)
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View,
    ) {
        if (dependency is Snackbar.SnackbarLayout) {
            snackbarHeight = 0f
        }
        updateFab(child)
    }

    private fun updateSnackbar(child: View, snackbarLayout: Snackbar.SnackbarLayout) {
        snackbarLayout.translationY = -bottomNavHeight + bottomInset - snackBarBottomMargin
    }

    private fun updateFab(fab: View): Boolean {
        val oldTranslation = fab.translationY
        val newTranslation = -bottomNavHeight - snackbarHeight
        fab.translationY = newTranslation

        return oldTranslation != newTranslation
    }
}
