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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.reddit_objects.UserInfo
import com.idunnololz.summit.util.PreferenceUtil
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils

class SettingsFragment : BasePreferenceFragment() {

    companion object {
        private const val TAG = "SettingsFragment"

        private const val PREF_KEY_ADD_PREFERENCE = "PREF_KEY_ADD_PREFERENCE"
        private const val PREF_KEY_ACCOUNT = "PREF_KEY_ACCOUNT"
    }

    private val authManager = RedditAuthManager.instance

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
            setupForFragment<SettingsFragment>()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().insetRootViewAutomatically(viewLifecycleOwner, view)

        userInfoViewModel.userInfoLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    refreshAccountsUi(it.data)
                }
            }
        })
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
        val userInfo = authManager.getCachedUserInfo()
        refreshAccountsUi(userInfo)

        userInfoViewModel.fetchUserInfo()
    }

    private fun removePreference(key: String) {
        findPreference<Preference>(key)?.let {
            it.parent?.removePreference(it)
        }
    }

    private fun ensureAddAccountPref(): Preference {
        removePreference(PREF_KEY_ACCOUNT)
        findPreference<Preference>(PREF_KEY_ADD_PREFERENCE)?.let {
            return it
        }

        val accountCategory = findPreference<PreferenceCategory>(R.string.pref_key_account)
        val addAccountPref = Preference(requireContext()).apply {
            title = getString(R.string.add_account)
            key = PREF_KEY_ADD_PREFERENCE
            isIconSpaceReserved = false

            setOnPreferenceClickListener {
                if (!authManager.showPreSignInIfNeeded(childFragmentManager)) {
                    loadUserInfo()
                }

                true
            }
        }
        accountCategory.addPreference(addAccountPref)
        return addAccountPref
    }

    private fun ensureAccountPref(): Preference {
        removePreference(PREF_KEY_ADD_PREFERENCE)
        findPreference<Preference>(PREF_KEY_ACCOUNT)?.let {
            return it
        }

        val accountCategory = findPreference<PreferenceCategory>(R.string.pref_key_account)
        val addAccountPref = Preference(requireContext()).apply {
            key = PREF_KEY_ACCOUNT
        }
        accountCategory.addPreference(addAccountPref)
        return addAccountPref
    }

    private fun refreshAccountsUi(userInfo: UserInfo?) {
        if (userInfo == null) {
            ensureAddAccountPref()
        } else {
            ensureAccountPref().apply {
                bindUserInfo(this@SettingsFragment, userInfo)

                setOnPreferenceClickListener {
                    val action =
                        SettingsFragmentDirections.actionSettingsFragmentToAccountSettingsFragment(
                            userInfo.id
                        )
                    findNavController().navigate(action)

                    true
                }
            }
        }
    }
}