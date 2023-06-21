package com.idunnololz.summit.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import com.idunnololz.summit.R
import com.idunnololz.summit.util.PreferenceUtil

class AccountSettingsFragment : BasePreferenceFragment() {

    companion object {
        private const val TAG = "AccountSettingsFragment"
    }

    private val args: AccountSettingsFragmentArgs by navArgs()

    private val viewModel: UserInfoViewModel by activityViewModels()

    private var signOutDialog: Dialog? = null

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == PreferenceUtil.KEY_OAUTH_TOKEN) {
                if (!sharedPreferences.contains(PreferenceUtil.KEY_OAUTH_TOKEN)) {
                    findNavController().navigateUp()
                    signOutDialog?.hide()
                }
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
            setupForFragment<AccountSettingsFragment>()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().insetViewAutomaticallyByMargins(viewLifecycleOwner, view)

        val signOutDialog = AlertDialog.Builder(
            context, R.style.Theme_App_Dialog_Alert_Transparent
        ).apply {
            setCancelable(false)
            setView(LayoutInflater.from(context).inflate(R.layout.sign_out_dialog, null, false))
        }.create().also {
            signOutDialog = it
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = PreferenceUtil.DEFAULT_PREF

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_account, rootKey)

        findPreference<Preference>(R.string.pref_key_sign_out).setOnPreferenceClickListener {
            true
        }
    }

    override fun onDestroyView() {
        PreferenceUtil.preferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )

        super.onDestroyView()
    }
}