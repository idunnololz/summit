package com.idunnololz.summit.settings.notifications

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingNavigationBinding
import com.idunnololz.summit.databinding.FragmentSettingNotificationsBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.NavigationSettings
import com.idunnololz.summit.settings.NotificationSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingNotificationsFragment :
    BaseFragment<FragmentSettingNotificationsBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: NotificationSettings

    private val viewModel: SettingNotificationsViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            setNotificationsEnabled(isGranted)

            updateRendering()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingNotificationsBinding.inflate(inflater, container, false))

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        settings.isNotificationsEnabled.bindTo(
            b = binding.notifications,
            { preferences.isNotificationsOn },
            {

                val newState = !preferences.isNotificationsOn

                if (newState &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    with(NotificationManagerCompat.from(context)) {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )

                            return@with
                        }

                        setNotificationsEnabled(true)
                    }
                } else {
                    setNotificationsEnabled(newState)
                }
            },
        )
        binding.notifications.root.visibility = View.GONE
    }

    private fun setNotificationsEnabled(newState: Boolean) {
        preferences.isNotificationsOn = newState
        preferences.notificationsLastUpdateMs = System.currentTimeMillis()
        viewModel.onNotificationSettingsChanged()
        updateRendering()
    }
}