package com.idunnololz.summit.lemmy.modlogs.filter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.api.LemmyApiClient.Companion.DEFAULT_LEMMY_INSTANCES
import com.idunnololz.summit.databinding.DialogFragmentModLogsFilterBinding
import com.idunnololz.summit.emoji.TextEmojiEditDialogFragment
import com.idunnololz.summit.lemmy.modlogs.ModEvent
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.setupToolbar
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

class ModLogsFilterDialogFragment : BaseDialogFragment<DialogFragmentModLogsFilterBinding>() {

  companion object {

    private const val USER_REQUEST_KEY = "USER_REQUEST_KEY"
    private const val MOD_REQUEST_KEY = "MOD_REQUEST_KEY"

    fun show(fragmentManager: FragmentManager) {
      ModLogsFilterDialogFragment()
        .show(fragmentManager, "ModLogsFilterDialogFragment")
    }
  }

  override fun onStart() {
    super.onStart()
    setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentModLogsFilterBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    fun onResult(k: String, bundle: Bundle) {
      val result = bundle.getParcelableCompat<PersonPickerDialogFragment.Result>(
        PersonPickerDialogFragment.REQUEST_KEY_RESULT,
      )

      if (result != null) {
        if (k == USER_REQUEST_KEY) {
          binding.userFilterEditText.setText(result.personRef.fullName)
        } else {
          binding.modFilterEditText.setText(result.personRef.fullName)
        }
      }
    }

    childFragmentManager.setFragmentResultListener(
      USER_REQUEST_KEY, viewLifecycleOwner
    ) { k, bundle ->
      onResult(k, bundle)
    }
    childFragmentManager.setFragmentResultListener(
      MOD_REQUEST_KEY, viewLifecycleOwner
    ) { k, bundle ->
      onResult(k, bundle)
    }

    with (binding) {
      setupToolbar(toolbar, getString(R.string.filter_mod_logs))

      val options = ModEvent::class.sealedSubclasses
      val optionStrings = options.mapNotNull {
        when (it as? ModEvent) {
          is ModEvent.AdminPurgeCommentViewEvent -> TODO()
          is ModEvent.AdminPurgeCommunityViewEvent -> TODO()
          is ModEvent.AdminPurgePersonViewEvent -> TODO()
          is ModEvent.AdminPurgePostViewEvent -> TODO()
          is ModEvent.ModAddCommunityViewEvent -> TODO()
          is ModEvent.ModAddViewEvent -> TODO()
          is ModEvent.ModBanFromCommunityViewEvent -> TODO()
          is ModEvent.ModBanViewEvent -> TODO()
          is ModEvent.ModFeaturePostViewEvent -> TODO()
          is ModEvent.ModHideCommunityViewEvent -> TODO()
          is ModEvent.ModLockPostViewEvent -> TODO()
          is ModEvent.ModRemoveCommentViewEvent -> TODO()
          is ModEvent.ModRemoveCommunityViewEvent -> TODO()
          is ModEvent.ModRemovePostViewEvent -> TODO()
          is ModEvent.ModTransferCommunityViewEvent -> TODO()
          null -> null
        }
        when (it) {
          ModEvent.AdminPurgeCommentViewEvent::class -> context.getString(R.string.purge_comment)
          ModEvent.AdminPurgeCommunityViewEvent::class -> context.getString(R.string.purge_community)
          ModEvent.AdminPurgePersonViewEvent::class -> context.getString(R.string.purge_person)
          ModEvent.AdminPurgePostViewEvent::class -> context.getString(R.string.purge_post)
          ModEvent.ModAddCommunityViewEvent::class -> context.getString(R.string.add_mod)
          ModEvent.ModAddViewEvent::class -> context.getString(R.string.add_admin)
          ModEvent.ModBanFromCommunityViewEvent::class -> context.getString(R.string.ban_user_from_community)
          ModEvent.ModBanViewEvent::class -> context.getString(R.string.ban_user_from_site)
          ModEvent.ModFeaturePostViewEvent::class -> context.getString(R.string.feature_post)
          ModEvent.ModHideCommunityViewEvent::class -> context.getString(R.string.hide_community)
          ModEvent.ModLockPostViewEvent::class -> context.getString(R.string.lock_post)
          ModEvent.ModRemoveCommentViewEvent::class -> context.getString(R.string.remove_comment)
          ModEvent.ModRemoveCommunityViewEvent::class -> context.getString(R.string.remove_community)
          ModEvent.ModRemovePostViewEvent::class -> context.getString(R.string.remove_post)
          ModEvent.ModTransferCommunityViewEvent::class -> context.getString(R.string.transferred_ownership_of_community)
          else -> null
        }
      }

      actionFilterText.setAdapter(
        ArrayAdapter(
          context,
          R.layout.auto_complete_simple_item,
          optionStrings,
        ),
      )

      userFilterEditText.setOnClickListener {
        PersonPickerDialogFragment.show(
          childFragmentManager,
          userFilterEditText.text?.toString(),
          USER_REQUEST_KEY,
        )
      }

      userFilterEditText.setOnClickListener {
        PersonPickerDialogFragment.show(
          childFragmentManager,
          userFilterEditText.text?.toString(),
          MOD_REQUEST_KEY,
        )
      }
    }
  }
}