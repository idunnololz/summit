package com.idunnololz.summit.settings

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.Utils

class OldSettingsFragment : BasePreferenceFragment() {

    companion object {
        private const val TAG = "SettingsFragment"

        private const val PREF_KEY_ADD_PREFERENCE = "PREF_KEY_ADD_PREFERENCE"
        private const val PREF_KEY_ACCOUNT = "PREF_KEY_ACCOUNT"
    }

    private val userInfoViewModel: UserInfoViewModel by activityViewModels()

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener a@{ sharedPreferences, key ->
            if (key == PreferenceUtil.KEY_OAUTH_TOKEN) {
                loadUserInfo()
            } else if (key == "pref_key_theme") {
                val context = context ?: return@a
                Handler().postDelayed({
                    Utils.triggerRebirth(context)
                }, 300)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        PreferenceUtil.preferences.registerOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )

        requireMainActivity().apply {
            setupForFragment<OldSettingsFragment>()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().insetViewAutomaticallyByMargins(viewLifecycleOwner, view)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PreferenceUtil.DEFAULT_PREF

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val sharedPref = PreferenceUtil.preferences

        findPreference<Preference>("pref_key_theme")?.setOnPreferenceChangeListener { preference, newValue ->
            sharedPref.edit().putInt(
                PreferenceUtil.KEY_THEME, when (newValue as String) {
                    "day" -> AppCompatDelegate.MODE_NIGHT_NO
                    "night" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    } else {
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    }
                }
            ).apply()

            true
        }

        findPreference<Preference>(R.string.pref_key_build_version).apply {
            summary = getString(R.string.build_version, BuildConfig.VERSION_NAME)
        }
    }

    override fun onResume() {
        super.onResume()

        loadUserInfo()
    }

    override fun onDestroyView() {
        PreferenceUtil.preferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )

        super.onDestroyView()
    }


    fun loadUserInfo() {
    }

    private fun removePreference(key: String) {
        findPreference<Preference>(key)?.let {
            it.parent?.removePreference(it)
        }
    }
}