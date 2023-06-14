package com.idunnololz.summit.settings

import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.idunnololz.summit.main.MainActivity

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    fun requireMainActivity(): MainActivity = requireActivity() as MainActivity

    protected fun <T : Preference> findPreference(@StringRes key: Int): T {
        return checkNotNull(findPreference<T>(getString(key)))
    }
}
