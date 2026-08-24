package com.idunnololz.summit.inbox

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.lemmy.post.goBack
import com.idunnololz.summit.util.BaseFragment

interface InboxChildFragment {

  val <T: ViewBinding> BaseFragment<T>.inboxTabbedFragment
    get() = parentFragment as? InboxTabbedFragment

  fun <T: ViewBinding> BaseFragment<T>.setupBackPressedDispatcher() {
    // DO NOT add back press dispatchers in Fragment.onCreate(). For some reason it doesn't work
    // when the activity is killed and recreated.
    requireSummitActivity().onBackPressedDispatcher
      .addCallback(
        this,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            inboxTabbedFragment?.slidingPaneController?.unlockNavBar()
            inboxTabbedFragment?.closeMessage()
          }

          var currentInboxTabbedFragment: InboxTabbedFragment? = null

          override fun handleOnBackStarted(backEvent: BackEventCompat) {
            super.handleOnBackStarted(backEvent)

            currentInboxTabbedFragment = inboxTabbedFragment

            currentInboxTabbedFragment?.slidingPaneController?.apply {
              lockNavBar()
              setPanelOffset(0.25f)
            }
          }

          override fun handleOnBackProgressed(backEvent: BackEventCompat) {}

          override fun handleOnBackCancelled() {
            if (!isBindingAvailable()) return

            currentInboxTabbedFragment?.slidingPaneController?.apply {
              setPanelOffset(0f)
              binding.root.postDelayed(
                {
                  unlockNavBar()
                },
                200,
              )
            }
            currentInboxTabbedFragment = null
          }
        },
      )
  }
}