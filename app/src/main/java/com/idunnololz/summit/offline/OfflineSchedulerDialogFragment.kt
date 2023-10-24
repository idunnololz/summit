package com.idunnololz.summit.offline

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentOfflineSchedulerBinding
import com.idunnololz.summit.util.AnimationUtils
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.view.DayPicker
import java.util.*

class OfflineSchedulerDialogFragment : BaseDialogFragment<DialogFragmentOfflineSchedulerBinding>() {

    companion object {

        private const val TAG = "OfflineScheduler"

        fun newInstance(): OfflineSchedulerDialogFragment = OfflineSchedulerDialogFragment()
    }

    interface OfflineSchedulerListener {
        fun onOfflineScheduleChanged(enable: Boolean, recurringEvent: RecurringEvent)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.Theme_App_Dialog)

        val preferences = PreferenceUtil.preferences

        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.dialog_fragment_offline_scheduler, null)
        val enableSwitch: Switch = rootView.findViewById(R.id.enable_switch)
        val dayPicker: DayPicker = rootView.findViewById(R.id.day_picker)
        val timePicker: TimePicker = rootView.findViewById(R.id.time_picker)
        val disableOverlay: View = rootView.findViewById(R.id.disable_overlay)

        val disableOverlayAnimationController =
            AnimationUtils.makeAnimationControllerFor(disableOverlay)

        val bgColor = context.getColorCompat(R.color.colorSurface)
        disableOverlay.setBackgroundColor(
            Color.argb(
                200,
                Color.red(bgColor),
                Color.green(bgColor),
                Color.blue(bgColor),
            ),
        )

        fun onEnableChanged() {
            if (enableSwitch.isChecked) {
                dayPicker.isEnabled = true
                recursivelyEnable(timePicker, true)
                disableOverlayAnimationController.hide()
            } else {
                dayPicker.isEnabled = false
                recursivelyEnable(timePicker, false)
                disableOverlayAnimationController.show()
                disableOverlay.setOnClickListener { }
            }
        }

        enableSwitch.setOnCheckedChangeListener { _, _ ->
            onEnableChanged()
        }
        enableSwitch.isChecked =
            preferences.getBoolean(PreferenceUtil.KEY_ENABLE_OFFLINE_SCHEDULE, false)
        onEnableChanged()

        preferences.getString(PreferenceUtil.KEY_OFFLINE_SCHEDULE, null)?.let {
            val savedRecurringEvent = RecurringEvent.fromString(it)
            dayPicker.setSelectedDays(savedRecurringEvent.daysOfWeek)
            timePicker.currentHour = savedRecurringEvent.hourOfDay
            timePicker.currentMinute = savedRecurringEvent.minuteOfHour
        }

        builder.setView(rootView)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val cb = (targetFragment ?: parentFragment) as? OfflineSchedulerListener

            val enabled = enableSwitch.isChecked
            val recurringEvent = RecurringEvent(
                daysOfWeek = dayPicker.getSelectedDays(),
                hourOfDay = timePicker.currentHour,
                minuteOfHour = timePicker.currentMinute,
            )

            PreferenceUtil.preferences.edit()
                .putBoolean(PreferenceUtil.KEY_ENABLE_OFFLINE_SCHEDULE, enabled)
                .putString(PreferenceUtil.KEY_OFFLINE_SCHEDULE, recurringEvent.serializeToString())
                .apply()

            OfflineScheduleManager.instance.onAlarmChanged()

            cb?.onOfflineScheduleChanged(
                enable = enabled,
                recurringEvent = recurringEvent,
            )
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        return builder.create()
    }

    private fun recursivelyEnable(view: View, enable: Boolean) {
        val toDisable = LinkedList<View>()
        toDisable.add(view)

        while (toDisable.isNotEmpty()) {
            val view = toDisable.pop()

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    toDisable.add(view.getChildAt(i))
                }
            }

            view.isEnabled = enable
        }
    }
}
