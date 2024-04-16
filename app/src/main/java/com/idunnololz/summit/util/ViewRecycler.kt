package com.idunnololz.summit.util

import android.view.View
import android.view.ViewGroup
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

/**
 * The ViewRecycler facilitates reuse of views across layouts.
 */
class ViewRecycler<T : View> {

    private val views: HashMap<Int, Stack<T>> = HashMap()

    /**
     * Add a view to the ViewRecycler. This view may be reused in the function
     * [.getRecycledView]
     *
     * @param view
     * A view to add to the ViewRecycler. It can no longer be used.
     * @param type
     * the type of the view.
     */
    fun addRecycledView(view: T, type: Int = 0) {
        if (view.parent != null) {
            throw RuntimeException(
                "Attempted to recycle view with parent. This may cause memory leaks.",
            )
        }
        views.getOrPut(type) { Stack() }.push(view)
    }

    /**
     * Returns, if exists, a view of the type `typeView`.
     *
     * @param viewType
     * the type of view that you want.
     * @return a view of the type `typeView`. `null` if
     * not found.
     */
    fun getRecycledView(viewType: Int = 0): T? {
        return try {
            views[viewType]?.pop()
        } catch (e: EmptyStackException) {
            null
        }
    }

    fun ensureViewGroupHasChildren(viewGroup: ViewGroup, numChildren: Int, viewFactory: () -> T) {
        val newViewsNeeded = numChildren - viewGroup.childCount

        if (newViewsNeeded > 0) {
            for (i in 0 until newViewsNeeded) {
                val view = viewFactory()
                viewGroup.addView(view)
            }
        } else {
            for (i in viewGroup.childCount - 1 downTo numChildren) {
                val view = viewGroup.getChildAt(i)
                viewGroup.removeViewAt(i)
                @Suppress("UNCHECKED_CAST")
                addRecycledView(view as T)
            }
        }
    }
}
