package com.idunnololz.summit.lemmy.modlogs.modEvent

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.DialogFragmentModEventBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.getName2
import com.idunnololz.summit.lemmy.modlogs.ModEvent
import com.idunnololz.summit.lemmy.modlogs.getColor
import com.idunnololz.summit.lemmy.modlogs.getIconRes
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.durationToPretty
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getColorStateListFromAttribute
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import javax.inject.Inject
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class ModEventDialogFragment : BaseDialogFragment<DialogFragmentModEventBinding>() {

  companion object {
    fun show(fragmentManager: FragmentManager, modEvent: ModEvent) {
      ModEventDialogFragment()
        .apply {
          arguments = ModEventDialogFragmentArgs(
            modEventJson = Json.encodeToString(modEvent),
          ).toBundle()
        }
        .show(fragmentManager, "ModEventDialogFragment")
    }
  }

  private val args: ModEventDialogFragmentArgs by navArgs()

  @Inject
  lateinit var apiClient: AccountAwareLemmyClient

  private val invalidArgsDialogLauncher = newAlertDialogLauncher("error_invalid_args") {
    dismiss()
  }

  override fun onStart() {
    super.onStart()

    setSizeDynamically(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(DialogFragmentModEventBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()
    val modEvent: ModEvent? = try {
      Json.decodeFromString(args.modEventJson)
    } catch (e: Exception) {
      null
    }

    with(binding) {
      setupToolbar(toolbar, "")

      if (modEvent == null) {
        invalidArgsDialogLauncher.launchDialog {
          messageResId = R.string.error_invalid_arguments
        }
      }

      var lastViewId = R.id.title

      fun addField(fieldName: String, fieldValue: String, onClick: (() -> Unit)? = null) {
        val titleView = TextView(
          context,
          null,
          com.google.android.material.R.attr.textAppearanceLabelSmall,
        ).apply {
          id = View.generateViewId()
          layoutParams = ConstraintLayout.LayoutParams(
            0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
          ).apply {
            startToStart = R.id.title
            endToEnd = R.id.title
            topToBottom = lastViewId
            topMargin = context.resources.getDimensionPixelOffset(R.dimen.padding)
          }
          setTextColor(context.getColorCompat(R.color.colorTextFaint))
          text = fieldName
          if (onClick != null) {
            setOnClickListener { onClick() }
          }
        }
        val valueView = TextView(
          context,
          null,
          com.google.android.material.R.attr.textAppearanceBodyMedium,
        ).apply {
          id = View.generateViewId()
          layoutParams = ConstraintLayout.LayoutParams(
            0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
          ).apply {
            startToStart = R.id.title
            endToEnd = R.id.title
            topToBottom = titleView.id
          }
          text = fieldValue
          if (onClick != null) {
            context.getColorStateListFromAttribute(
              androidx.appcompat.R.attr.colorPrimary,
            )?.let {
              setTextColor(it)
            }

            setOnClickListener { onClick() }
          }
        }
        content.addView(titleView)
        content.addView(valueView)
        lastViewId = valueView.id
      }

      val hiddenString = context.getString(R.string.hidden)
      fun addAdminField(admin: Person?) {
        if (admin != null) {
          addField(
            context.getString(R.string.admin),
            admin.fullName,
          ) {
            getMainActivity()?.launchPage(
              admin.toPersonRef(),
            )
            dismiss()
          }
        } else {
          addField(
            context.getString(R.string.admin),
            admin?.fullName ?: hiddenString,
          )
        }
      }
      fun addModeratorField(moderator: Person?) {
        if (moderator != null) {
          addField(
            context.getString(R.string.moderator),
            moderator.fullName,
          ) {
            getMainActivity()?.launchPage(
              moderator.toPersonRef(),
            )
            dismiss()
          }
        } else {
          addField(
            context.getString(R.string.admin),
            hiddenString,
          )
        }
      }

      if (modEvent != null) {
        val modEventColor = modEvent.getColor(context)
        val containerColor = context.getColorFromAttribute(
          com.google.android.material.R.attr.colorSurfaceContainerHigh,
        )
        icon.setImageResource(modEvent.getIconRes())
        icon.imageTintList = ColorStateList.valueOf(modEventColor)
        icon.backgroundTintList = ColorStateList.valueOf(
          ColorUtils.blendARGB(
            modEventColor,
            containerColor,
            0.9f,
          ),
        )

        when (modEvent) {
          is ModEvent.AdminPurgeCommentViewEvent -> {
            addAdminField(modEvent.event.admin)

            addField(
              context.getString(R.string.post),
              modEvent.event.post.name,
            ) {
              getMainActivity()?.launchPage(
                PostRef(apiClient.instance, modEvent.event.post.id),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.admin_purge_comment.reason ?: "",
            )
          }
          is ModEvent.AdminPurgeCommunityViewEvent -> {
            addAdminField(modEvent.event.admin)

            addField(
              context.getString(R.string.reason),
              modEvent.event.admin_purge_community.reason ?: "",
            )
          }
          is ModEvent.AdminPurgePersonViewEvent -> {
            addAdminField(modEvent.event.admin)

            addField(
              context.getString(R.string.reason),
              modEvent.event.admin_purge_person.reason ?: "",
            )
          }
          is ModEvent.AdminPurgePostViewEvent -> {
            addAdminField(modEvent.event.admin)

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.admin_purge_post.reason ?: "",
            )
          }
          is ModEvent.ModAddCommunityViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.user),
              modEvent.event.modded_person.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.modded_person.toPersonRef(),
              )
              dismiss()
            }
          }
          is ModEvent.ModAddViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.user),
              modEvent.event.modded_person.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.modded_person.toPersonRef(),
              )
              dismiss()
            }
          }
          is ModEvent.ModBanFromCommunityViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.user),
              modEvent.event.banned_person.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.banned_person.toPersonRef(),
              )
              dismiss()
            }
            val banExpires = modEvent.event.mod_ban_from_community.expires
            val banDuration = if (banExpires != null) {
              durationToPretty(
                dateStringToTs(banExpires) -
                  dateStringToTs(modEvent.event.mod_ban_from_community.when_),
              )
            } else {
              getString(R.string.permanently_banned)
            }
            addField(
              context.getString(R.string.duration),
              banDuration,
            )

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_ban_from_community.reason ?: "",
            )
          }
          is ModEvent.ModBanViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.user),
              modEvent.event.banned_person.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.banned_person.toPersonRef(),
              )
              dismiss()
            }

            val banExpires = modEvent.event.mod_ban.expires
            val banDuration = if (banExpires != null) {
              durationToPretty(
                dateStringToTs(banExpires) -
                  dateStringToTs(modEvent.event.mod_ban.when_),
              )
            } else {
              getString(R.string.permanently_banned)
            }
            addField(
              context.getString(R.string.duration),
              banDuration,
            )

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_ban.reason ?: "",
            )
          }
          is ModEvent.ModFeaturePostViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.post),
              modEvent.event.post.name,
            ) {
              getMainActivity()?.launchPage(
                PostRef(
                  apiClient.instance,
                  modEvent.event.post.id,
                ),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }
          }
          is ModEvent.ModHideCommunityViewEvent -> {
            addAdminField(modEvent.event.admin)

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_hide_community.reason ?: "",
            )
          }
          is ModEvent.ModLockPostViewEvent -> {
            val admin = modEvent.event.moderator
            addField(
              context.getString(R.string.moderator),
              admin?.fullName ?: hiddenString,
            ) {
              if (admin?.fullName != null) {
                getMainActivity()?.launchPage(
                  admin.toPersonRef(),
                )
                dismiss()
              }
            }

            addField(
              context.getString(R.string.post),
              modEvent.event.post.name,
            ) {
              getMainActivity()?.launchPage(
                PostRef(
                  apiClient.instance,
                  modEvent.event.post.id,
                ),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }
          }
          is ModEvent.ModRemoveCommentViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.comment),
              modEvent.event.comment.content,
            ) {
              getMainActivity()?.launchPage(
                CommentRef(
                  apiClient.instance,
                  modEvent.event.comment.id,
                ),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.user),
              modEvent.event.commenter.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.commenter.toPersonRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.post),
              modEvent.event.post.name,
            ) {
              getMainActivity()?.launchPage(
                PostRef(
                  apiClient.instance,
                  modEvent.event.post.id,
                ),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_remove_comment.reason ?: "",
            )
          }
          is ModEvent.ModRemoveCommunityViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }
            val banExpires = modEvent.event.mod_remove_community.expires
            val banDuration = if (banExpires != null) {
              durationToPretty(
                dateStringToTs(banExpires) -
                  dateStringToTs(modEvent.event.mod_remove_community.when_),
              )
            } else {
              getString(R.string.permanently_banned)
            }
            addField(
              context.getString(R.string.duration),
              banDuration,
            )

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_remove_community.reason ?: "",
            )
          }
          is ModEvent.ModRemovePostViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.post),
              modEvent.event.post.name,
            ) {
              getMainActivity()?.launchPage(
                PostRef(
                  apiClient.instance,
                  modEvent.event.post.id,
                ),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.reason),
              modEvent.event.mod_remove_post.reason ?: "",
            )
          }
          is ModEvent.ModTransferCommunityViewEvent -> {
            addModeratorField(modEvent.event.moderator)

            addField(
              context.getString(R.string.transferred_to),
              modEvent.event.modded_person.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.modded_person.toPersonRef(),
              )
              dismiss()
            }

            addField(
              context.getString(R.string.community),
              modEvent.event.community.fullName,
            ) {
              getMainActivity()?.launchPage(
                modEvent.event.community.toCommunityRef(),
              )
              dismiss()
            }
          }
        }

        overtext.text = context.getString(
          R.string.mod_action_format,
          modEvent.actionOrder.toString(),
        )
        val isUndoAction = when (modEvent) {
          is ModEvent.AdminPurgeCommentViewEvent -> false
          is ModEvent.AdminPurgeCommunityViewEvent -> false
          is ModEvent.AdminPurgePersonViewEvent -> false
          is ModEvent.AdminPurgePostViewEvent -> false
          is ModEvent.ModAddCommunityViewEvent -> modEvent.event.mod_add_community.removed
          is ModEvent.ModAddViewEvent -> modEvent.event.mod_add.removed
          is ModEvent.ModBanFromCommunityViewEvent -> !modEvent.event.mod_ban_from_community.banned
          is ModEvent.ModBanViewEvent -> !modEvent.event.mod_ban.banned
          is ModEvent.ModFeaturePostViewEvent -> !modEvent.event.mod_feature_post.featured
          is ModEvent.ModHideCommunityViewEvent -> !modEvent.event.mod_hide_community.hidden
          is ModEvent.ModLockPostViewEvent -> !modEvent.event.mod_lock_post.locked
          is ModEvent.ModRemoveCommentViewEvent -> !modEvent.event.mod_remove_comment.removed
          is ModEvent.ModRemoveCommunityViewEvent -> !modEvent.event.mod_remove_community.removed
          is ModEvent.ModRemovePostViewEvent -> !modEvent.event.mod_remove_post.removed
          is ModEvent.ModTransferCommunityViewEvent -> false
        }
        title.text = modEvent.actionType.getName2(context, isUndoAction)
      }
    }
  }
}
