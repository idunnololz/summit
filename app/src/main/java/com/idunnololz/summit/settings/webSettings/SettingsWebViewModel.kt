package com.idunnololz.summit.settings.webSettings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountImageGenerator
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.SaveUserSettings
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.settings.LemmyWebSettings
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsWebViewModel @Inject constructor(
    private val context: Application,
    private val accountManager: AccountManager,
    private val accountInfoManager: AccountInfoManager,
    private val lemmyApiClient: LemmyApiClient,
    private val lemmyWebSettings: LemmyWebSettings,
    private val state: SavedStateHandle,
    private val accountImageGenerator: AccountImageGenerator,
    private val directoryHelper: DirectoryHelper,
) : ViewModel() {

    var imagePickerKey: MutableLiveData<Int?> = state.getLiveData("imagePickerKey", null)
    val accountData = StatefulLiveData<AccountData>()
    val generatedLemming = StatefulLiveData<BitmapDrawable>()

    val saveUserSettings = StatefulLiveData<Unit>()
    val uploadImageStatus = StatefulLiveData<Pair<Int, String>>()

    fun fetchAccountInfo() {
        accountData.setIsLoading()

        viewModelScope.launch {
            accountInfoManager.fetchAccountInfo()
                .onSuccess { data ->

                    val account = accountManager.getAccounts().firstOrNull {
                        it.id == data.my_user?.local_user_view?.local_user?.person_id
                    }

                    if (account == null) {
                        accountData.postError(
                            RuntimeException(
                                "Unable to find account that matches the account info fetched.",
                            ),
                        )
                    } else {
                        updateValue(account, data)
                    }
                }
                .onFailure {
                    accountData.postError(it)
                }
        }
    }

    fun save(
        updatedValues: Map<Int, Any?>,
    ) {
        val accountData = accountData.valueOrNull ?: return
        val myUser = accountData.accountInfo.my_user ?: return
        val localUser = myUser.local_user_view.local_user
        val person = myUser.local_user_view.person

        var settings = // SaveUserSettings(auth = accountData.account.jwt)
            // We need to include all settings due to this server bug:
            // https://github.com/LemmyNet/lemmy-js-client/issues/144
            SaveUserSettings(
                display_name = person.name,
                bio = person.bio,
                email = localUser.email,
                auth = accountData.account.jwt,
                avatar = person.avatar,
                banner = person.banner,
                matrix_user_id = person.matrix_user_id,
                interface_language = localUser.interface_language,
                bot_account = person.bot_account,
                default_sort_type = localUser.default_sort_type,
                send_notifications_to_email = localUser.send_notifications_to_email,
                show_avatars = localUser.show_avatars,
                show_bot_accounts = localUser.show_bot_accounts,
                show_nsfw = localUser.show_nsfw,
                default_listing_type = localUser.default_listing_type,
                show_new_post_notifs = localUser.show_new_post_notifs,
                show_read_posts = localUser.show_read_posts,
                theme = localUser.theme,
                show_scores = localUser.show_scores,
                discussion_languages = null,
            )

        for ((key, value) in updatedValues) {
            when (key) {
                lemmyWebSettings.displayNameSetting.id -> {
                    settings = settings.copy(display_name = value as String?)
                }
                lemmyWebSettings.bioSetting.id -> {
                    settings = settings.copy(bio = value as String?)
                }
                lemmyWebSettings.emailSetting.id -> {
                    settings = settings.copy(email = value as String?)
                }
                lemmyWebSettings.matrixSetting.id -> {
                    settings = settings.copy(matrix_user_id = value as String?)
                }
                lemmyWebSettings.avatarSetting.id -> {
                    settings = settings.copy(avatar = value as String?)
                }
                lemmyWebSettings.bannerSetting.id -> {
                    settings = settings.copy(banner = value as String?)
                }
                lemmyWebSettings.defaultSortType.id -> {
                    val order = value as Int? ?: continue
                    settings = settings.copy(
                        default_sort_type = idToSortOrder(order)?.toApiSortOrder(),
                    )
                }
                lemmyWebSettings.showNsfwSetting.id -> {
                    settings = settings.copy(
                        show_nsfw = value as Boolean?,
                    )
                }
                lemmyWebSettings.showReadPostsSetting.id -> {
                    settings = settings.copy(
                        show_read_posts = value as Boolean?,
                    )
                }
                lemmyWebSettings.botAccountSetting.id -> {
                    settings = settings.copy(
                        bot_account = value as Boolean?,
                    )
                }
                lemmyWebSettings.showBotAccountsSetting.id -> {
                    settings = settings.copy(
                        show_bot_accounts = value as Boolean?,
                    )
                }
                lemmyWebSettings.sendNotificationsToEmailSetting.id -> {
                    settings = settings.copy(
                        send_notifications_to_email = value as Boolean?,
                    )
                }
                else -> {
                    // do nothing
                }
            }
        }

        saveUserSettings.setIsLoading()

        viewModelScope.launch {
            lemmyApiClient.changeInstance(accountData.account.instance)
            lemmyApiClient.saveUserSettings(settings)
                .onSuccess {
                    saveUserSettings.postValue(Unit)
                }
                .onFailure {
                    saveUserSettings.postError(it)
                }
        }
    }

    private fun updateValue(account: Account, data: GetSiteResponse) {
        val myUser = data.my_user ?: return
        val localUser = myUser.local_user_view.local_user
        val person = myUser.local_user_view.person

        accountData.postValue(
            AccountData(
                data,
                account,
                lemmyWebSettings.allSettings,
                mapOf(
                    lemmyWebSettings.instanceSetting.id to account.instance,
                    lemmyWebSettings.displayNameSetting.id to person.name,
                    lemmyWebSettings.bioSetting.id to person.bio,
                    lemmyWebSettings.emailSetting.id to localUser.email,
                    lemmyWebSettings.matrixSetting.id to person.matrix_user_id,
                    lemmyWebSettings.avatarSetting.id to person.avatar,
                    lemmyWebSettings.bannerSetting.id to person.banner,
                    lemmyWebSettings.defaultSortType.id to localUser.default_sort_type?.toId(),
                    lemmyWebSettings.showNsfwSetting.id to localUser.show_nsfw,
                    lemmyWebSettings.showReadPostsSetting.id to localUser.show_read_posts,
                    lemmyWebSettings.botAccountSetting.id to person.bot_account,
                    lemmyWebSettings.showBotAccountsSetting.id to localUser.show_bot_accounts,
                    lemmyWebSettings.sendNotificationsToEmailSetting.id to localUser.send_notifications_to_email,
                    lemmyWebSettings.blockSettings.id to Unit,
                ),
            ),
        )
    }

    fun uploadImage(uri: Uri) {
        val imagePickerKey = imagePickerKey.value ?: return
        val fullAccount = accountInfoManager.currentFullAccount.value ?: return

        uploadImageStatus.setIsLoading()
        viewModelScope.launch {
            lemmyApiClient.changeInstance(fullAccount.account.instance)

            context.contentResolver
                .openInputStream(uri)
                .use {
                    if (it == null) {
                        return@use Result.failure(RuntimeException("file_not_found"))
                    }
                    lemmyApiClient.uploadImage(fullAccount.account, "asdf", it)
                }
                .onSuccess {
                    uploadImageStatus.postValue(imagePickerKey to it.url)
                }
                .onFailure {
                    uploadImageStatus.postError(it)
                }
        }
    }

    fun generateLemming(account: Account) {
        generatedLemming.setIsLoading()

        viewModelScope.launch {
            val drawable = accountImageGenerator.generateDrawableForKey(
                "${account.name}@${account.id}@${account.instance}",
            ) as BitmapDrawable

            val file = File(
                directoryHelper.imagesDir,
                "${account.name}@${account.id}@${account.instance}.png",
            )

            drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())

            uploadImage(file.toUri())

            generatedLemming.postValue(drawable)
        }
    }

    class AccountData(
        val accountInfo: GetSiteResponse,
        val account: Account,
        val settings: List<SettingItem>,
        val defaultValues: Map<Int, Any?>,
    )
}
