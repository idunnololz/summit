package com.idunnololz.summit.util

import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

fun setupBottomSheetAndShow(
  bottomSheet: View,
  bottomSheetContainerInner: View,
  overlay: View,
  onClose: () -> Unit,
  expandFully: Boolean = false,
) {
  val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
    isHideable = true
    state = BottomSheetBehavior.STATE_HIDDEN
    peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO

    if (expandFully) {
      skipCollapsed = true
    }
  }
  overlay.setOnClickListener {
    bottomSheetBehavior.setState(
      BottomSheetBehavior.STATE_HIDDEN,
    )
  }

//    bottomInset.observeForever {
//        recyclerView.updatePadding(bottom = it)
//    }
//    topInset.observeForever { topInset ->
//        binding.bottomSheetContainerInner.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//            topMargin = topInset
//        }
//    }

  bottomSheet.postDelayed(
    {
      if (bottomSheetContainerInner.width > bottomSheetBehavior.maxWidth) {
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      } else if (expandFully) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      } else {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
      }

      bottomSheetBehavior.addBottomSheetCallback(
        object : BottomSheetBehavior.BottomSheetCallback() {
          override fun onStateChanged(bottomSheet1: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
              onClose.invoke()
            }
          }

          override fun onSlide(bottomSheet1: View, slideOffset: Float) {
            if (java.lang.Float.isNaN(slideOffset)) {
              overlay.alpha = 1f
            } else {
              overlay.alpha = 1 + slideOffset
            }
          }
        },
      )
    },
    100,
  )
}

fun fixBottomSheetFling(
  bottomSheetBehavior: BottomSheetBehavior<FrameLayout>,
  recyclerView: RecyclerView,
) {
  recyclerView.isNestedScrollingEnabled = false
  recyclerView.overScrollMode = OVER_SCROLL_NEVER

  recyclerView.addOnScrollListener(
    object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val position = (recyclerView.layoutManager as? LinearLayoutManager)
          ?.findFirstCompletelyVisibleItemPosition()

        bottomSheetBehavior.isDraggable = position == 0
      }
    },
  )
}