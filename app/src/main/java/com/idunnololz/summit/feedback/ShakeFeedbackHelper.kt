package com.idunnololz.summit.feedback

import android.app.Activity
import android.content.Context
import android.hardware.SensorManager
import androidx.core.view.HapticFeedbackConstantsCompat
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.ShakeDetector
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class ShakeFeedbackHelper @Inject constructor(
    private val activity: Activity,
) {

    companion object {
        private const val SHAKE_COOLDOWN_MS = 4_000
    }

    private var lastShakeTs = 0L

    private val shakeDetector =
        ShakeDetector(
            object : ShakeDetector.Listener {
                override fun hearShake() {
                    val elapsed = System.currentTimeMillis() - lastShakeTs

                    if (elapsed < SHAKE_COOLDOWN_MS) {
                        return
                    }

                    val mainActivity = (activity as? MainActivity) ?: return

                    val alreadyShowingFeedback = mainActivity.supportFragmentManager.fragments
                        .any { it is HelpAndFeedbackDialogFragment && it.isVisible }

                    if (alreadyShowingFeedback) {
                        return
                    }

                    mainActivity.getRootView().performHapticFeedbackCompat(
                        HapticFeedbackConstantsCompat.LONG_PRESS,
                    )

                    lastShakeTs = System.currentTimeMillis()

                    HelpAndFeedbackDialogFragment.show(mainActivity.supportFragmentManager)
                }
            },
        )

    fun start() {
        (activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.let {
            shakeDetector.start(it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        shakeDetector.stop()
    }
}
