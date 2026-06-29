# AccountApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**blockCommunity**](AccountApi.md#blockCommunity) | **POST** /api/v4/account/block/community | Block a community. |
| [**blockPerson**](AccountApi.md#blockPerson) | **POST** /api/v4/account/block/person | Block a person. |
| [**changePassword**](AccountApi.md#changePassword) | **PUT** /api/v4/account/auth/change_password | Change your user password. |
| [**changePasswordAfterReset**](AccountApi.md#changePasswordAfterReset) | **POST** /api/v4/account/auth/password_change | Change your password from an email / token based reset. |
| [**createRegistrationInvitation**](AccountApi.md#createRegistrationInvitation) | **POST** /api/v4/account/invite | Create a new registration invitation |
| [**deleteAccount**](AccountApi.md#deleteAccount) | **DELETE** /api/v4/account | Delete your account. |
| [**deleteMedia**](AccountApi.md#deleteMedia) | **DELETE** /api/v4/account/media | Delete media for your account. |
| [**deleteUserAvatar**](AccountApi.md#deleteUserAvatar) | **DELETE** /api/v4/account/avatar | Delete the user avatar. |
| [**deleteUserBanner**](AccountApi.md#deleteUserBanner) | **DELETE** /api/v4/account/banner | Delete the user banner. |
| [**editTotp**](AccountApi.md#editTotp) | **POST** /api/v4/account/auth/totp/edit | Enable / Disable TOTP / two-factor authentication.  To enable, you need to first call &#x60;/account/auth/totp/generate&#x60; and then pass a valid token to this.  Disabling is only possible if 2FA was previously enabled. Again it is necessary to pass a valid token. |
| [**exportUserSettings**](AccountApi.md#exportUserSettings) | **GET** /api/v4/account/settings/export | Export a backup of your user settings.  Export a backup of your user settings, including your saved content, followed communities, and blocks. |
| [**generateTotpSecret**](AccountApi.md#generateTotpSecret) | **POST** /api/v4/account/auth/totp/generate | Generate a TOTP / two-factor secret.  Generate a TOTP / two-factor secret. Afterwards you need to call &#x60;/account/auth/totp/edit&#x60; with a valid token to enable it. |
| [**getCaptcha**](AccountApi.md#getCaptcha) | **GET** /api/v4/account/auth/get_captcha | Fetch a Captcha. |
| [**getMyUser**](AccountApi.md#getMyUser) | **GET** /api/v4/account | Get data of current user. |
| [**getUnreadCounts**](AccountApi.md#getUnreadCounts) | **GET** /api/v4/account/unread_counts | Returns the amount of unread items of various types. For normal users this means * the number of unread notifications, mods and admins get additional unread counts for reports, registration applications and pending follows to private communities. |
| [**importUserSettings**](AccountApi.md#importUserSettings) | **POST** /api/v4/account/settings/import | Import a backup of your user settings. |
| [**listLogins**](AccountApi.md#listLogins) | **GET** /api/v4/account/login/list | List login tokens for your user |
| [**listMedia**](AccountApi.md#listMedia) | **GET** /api/v4/account/media/list | List all the media for your account. |
| [**listNotifications**](AccountApi.md#listNotifications) | **GET** /api/v4/account/notification/list | Get your inbox (replies, comment mentions, post mentions, and messages) |
| [**listPersonHidden**](AccountApi.md#listPersonHidden) | **GET** /api/v4/account/hidden | List your hidden content. |
| [**listPersonLiked**](AccountApi.md#listPersonLiked) | **GET** /api/v4/account/liked | List your liked content. |
| [**listPersonRead**](AccountApi.md#listPersonRead) | **GET** /api/v4/account/read | List your read content. |
| [**listPersonSaved**](AccountApi.md#listPersonSaved) | **GET** /api/v4/account/saved | List your saved content. |
| [**listRegistrationInvitations**](AccountApi.md#listRegistrationInvitations) | **GET** /api/v4/account/invite/list | Revoke a previously created registration invitation |
| [**login**](AccountApi.md#login) | **POST** /api/v4/account/auth/login | Log into lemmy. |
| [**logout**](AccountApi.md#logout) | **POST** /api/v4/account/auth/logout | Invalidate the currently used auth token. |
| [**markAllNotificationsAsRead**](AccountApi.md#markAllNotificationsAsRead) | **POST** /api/v4/account/notification/mark_as_read/all | Mark all notifications as read. |
| [**markDonationDialogShown**](AccountApi.md#markDonationDialogShown) | **POST** /api/v4/account/donation_dialog_shown |  |
| [**markNotificationAsRead**](AccountApi.md#markNotificationAsRead) | **POST** /api/v4/account/notification/mark_as_read | Mark a notification as read. |
| [**register**](AccountApi.md#register) | **POST** /api/v4/account/auth/register | Register a new user. |
| [**resendVerificationEmail**](AccountApi.md#resendVerificationEmail) | **POST** /api/v4/account/auth/resend_verification_email | Resend a verification email. |
| [**resetPassword**](AccountApi.md#resetPassword) | **POST** /api/v4/account/auth/password_reset | Reset your password. |
| [**revokeRegistrationInvitation**](AccountApi.md#revokeRegistrationInvitation) | **DELETE** /api/v4/account/invite | Revoke a previously created registration invitation |
| [**saveUserSettings**](AccountApi.md#saveUserSettings) | **PUT** /api/v4/account/settings/save | Save your user settings. |
| [**uploadUserAvatar**](AccountApi.md#uploadUserAvatar) | **POST** /api/v4/account/avatar | Upload new user avatar. |
| [**uploadUserBanner**](AccountApi.md#uploadUserBanner) | **POST** /api/v4/account/banner | Upload new user banner. |
| [**userBlockInstanceCommunities**](AccountApi.md#userBlockInstanceCommunities) | **POST** /api/v4/account/block/instance/communities | Block an instance&#39;s communities as a user. |
| [**userBlockInstancePersons**](AccountApi.md#userBlockInstancePersons) | **POST** /api/v4/account/block/instance/persons | Block an instance&#39;s persons as a user. |
| [**validateAuth**](AccountApi.md#validateAuth) | **GET** /api/v4/account/validate_auth | Returns an error message if your auth token is invalid |
| [**verifyEmail**](AccountApi.md#verifyEmail) | **POST** /api/v4/account/auth/verify_email | Verify your email |


<a id="blockCommunity"></a>
# **blockCommunity**
> CommunityResponse blockCommunity(blockCommunity)

Block a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val blockCommunity : BlockCommunity =  // BlockCommunity | 
try {
    val result : CommunityResponse = apiInstance.blockCommunity(blockCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#blockCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#blockCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **blockCommunity** | [**BlockCommunity**](BlockCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="blockPerson"></a>
# **blockPerson**
> PersonResponse blockPerson(blockPerson)

Block a person.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val blockPerson : BlockPerson =  // BlockPerson | 
try {
    val result : PersonResponse = apiInstance.blockPerson(blockPerson)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#blockPerson")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#blockPerson")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **blockPerson** | [**BlockPerson**](BlockPerson.md)|  | |

### Return type

[**PersonResponse**](PersonResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="changePassword"></a>
# **changePassword**
> LoginResponse changePassword(changePassword)

Change your user password.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val changePassword : ChangePassword =  // ChangePassword | 
try {
    val result : LoginResponse = apiInstance.changePassword(changePassword)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#changePassword")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#changePassword")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **changePassword** | [**ChangePassword**](ChangePassword.md)|  | |

### Return type

[**LoginResponse**](LoginResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="changePasswordAfterReset"></a>
# **changePasswordAfterReset**
> SuccessResponse changePasswordAfterReset(changePasswordAfterReset)

Change your password from an email / token based reset.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val changePasswordAfterReset : ChangePasswordAfterReset =  // ChangePasswordAfterReset | 
try {
    val result : SuccessResponse = apiInstance.changePasswordAfterReset(changePasswordAfterReset)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#changePasswordAfterReset")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#changePasswordAfterReset")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **changePasswordAfterReset** | [**ChangePasswordAfterReset**](ChangePasswordAfterReset.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createRegistrationInvitation"></a>
# **createRegistrationInvitation**
> CreateInvitationResponse createRegistrationInvitation(createInvitation)

Create a new registration invitation

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val createInvitation : CreateInvitation =  // CreateInvitation | 
try {
    val result : CreateInvitationResponse = apiInstance.createRegistrationInvitation(createInvitation)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#createRegistrationInvitation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#createRegistrationInvitation")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createInvitation** | [**CreateInvitation**](CreateInvitation.md)|  | |

### Return type

[**CreateInvitationResponse**](CreateInvitationResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteAccount"></a>
# **deleteAccount**
> SuccessResponse deleteAccount(deleteAccount)

Delete your account.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val deleteAccount : DeleteAccount =  // DeleteAccount | 
try {
    val result : SuccessResponse = apiInstance.deleteAccount(deleteAccount)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#deleteAccount")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#deleteAccount")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteAccount** | [**DeleteAccount**](DeleteAccount.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteMedia"></a>
# **deleteMedia**
> SuccessResponse deleteMedia(deleteImageParamsI)

Delete media for your account.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : SuccessResponse = apiInstance.deleteMedia(deleteImageParamsI)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#deleteMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#deleteMedia")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteImageParamsI** | [**DeleteImageParamsI**](DeleteImageParamsI.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteUserAvatar"></a>
# **deleteUserAvatar**
> SuccessResponse deleteUserAvatar()

Delete the user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.deleteUserAvatar()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#deleteUserAvatar")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#deleteUserAvatar")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deleteUserBanner"></a>
# **deleteUserBanner**
> SuccessResponse deleteUserBanner()

Delete the user banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.deleteUserBanner()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#deleteUserBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#deleteUserBanner")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="editTotp"></a>
# **editTotp**
> EditTotpResponse editTotp(editTotp)

Enable / Disable TOTP / two-factor authentication.  To enable, you need to first call &#x60;/account/auth/totp/generate&#x60; and then pass a valid token to this.  Disabling is only possible if 2FA was previously enabled. Again it is necessary to pass a valid token.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val editTotp : EditTotp =  // EditTotp | 
try {
    val result : EditTotpResponse = apiInstance.editTotp(editTotp)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#editTotp")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#editTotp")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editTotp** | [**EditTotp**](EditTotp.md)|  | |

### Return type

[**EditTotpResponse**](EditTotpResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="exportUserSettings"></a>
# **exportUserSettings**
> kotlin.String exportUserSettings()

Export a backup of your user settings.  Export a backup of your user settings, including your saved content, followed communities, and blocks.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : kotlin.String = apiInstance.exportUserSettings()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#exportUserSettings")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#exportUserSettings")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**kotlin.String**

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="generateTotpSecret"></a>
# **generateTotpSecret**
> GenerateTotpSecretResponse generateTotpSecret()

Generate a TOTP / two-factor secret.  Generate a TOTP / two-factor secret. Afterwards you need to call &#x60;/account/auth/totp/edit&#x60; with a valid token to enable it.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : GenerateTotpSecretResponse = apiInstance.generateTotpSecret()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#generateTotpSecret")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#generateTotpSecret")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**GenerateTotpSecretResponse**](GenerateTotpSecretResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getCaptcha"></a>
# **getCaptcha**
> GetCaptchaResponse getCaptcha()

Fetch a Captcha.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : GetCaptchaResponse = apiInstance.getCaptcha()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#getCaptcha")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#getCaptcha")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**GetCaptchaResponse**](GetCaptchaResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getMyUser"></a>
# **getMyUser**
> MyUserInfo getMyUser()

Get data of current user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : MyUserInfo = apiInstance.getMyUser()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#getMyUser")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#getMyUser")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**MyUserInfo**](MyUserInfo.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getUnreadCounts"></a>
# **getUnreadCounts**
> UnreadCountsResponse getUnreadCounts()

Returns the amount of unread items of various types. For normal users this means * the number of unread notifications, mods and admins get additional unread counts for reports, registration applications and pending follows to private communities.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : UnreadCountsResponse = apiInstance.getUnreadCounts()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#getUnreadCounts")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#getUnreadCounts")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**UnreadCountsResponse**](UnreadCountsResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="importUserSettings"></a>
# **importUserSettings**
> SuccessResponse importUserSettings(userSettingsBackup)

Import a backup of your user settings.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val userSettingsBackup : UserSettingsBackup =  // UserSettingsBackup | 
try {
    val result : SuccessResponse = apiInstance.importUserSettings(userSettingsBackup)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#importUserSettings")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#importUserSettings")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userSettingsBackup** | [**UserSettingsBackup**](UserSettingsBackup.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listLogins"></a>
# **listLogins**
> ListLoginsResponse listLogins()

List login tokens for your user

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : ListLoginsResponse = apiInstance.listLogins()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listLogins")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listLogins")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ListLoginsResponse**](ListLoginsResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listMedia"></a>
# **listMedia**
> PagedResponseLocalImageView listMedia(limit, pageCursor)

List all the media for your account.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseLocalImageView = apiInstance.listMedia(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listMedia")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponseLocalImageView**](PagedResponseLocalImageView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listNotifications"></a>
# **listNotifications**
> PagedResponseNotificationView listNotifications(limit, pageCursor, creatorId, unreadOnly, type)

Get your inbox (replies, comment mentions, post mentions, and messages)

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val unreadOnly : kotlin.Boolean = true // kotlin.Boolean | 
val type : NotificationTypeFilter =  // NotificationTypeFilter | 
try {
    val result : PagedResponseNotificationView = apiInstance.listNotifications(limit, pageCursor, creatorId, unreadOnly, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listNotifications")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listNotifications")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **unreadOnly** | **kotlin.Boolean**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**NotificationTypeFilter**](.md)|  | [optional] |

### Return type

[**PagedResponseNotificationView**](PagedResponseNotificationView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersonHidden"></a>
# **listPersonHidden**
> PagedResponsePostView listPersonHidden(limit, pageCursor)

List your hidden content.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponsePostView = apiInstance.listPersonHidden(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listPersonHidden")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listPersonHidden")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponsePostView**](PagedResponsePostView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersonLiked"></a>
# **listPersonLiked**
> PagedResponsePostCommentCombinedView listPersonLiked(limit, pageCursor, likeType, type)

List your liked content.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val likeType : LikeType =  // LikeType | 
val type : PersonContentType =  // PersonContentType | 
try {
    val result : PagedResponsePostCommentCombinedView = apiInstance.listPersonLiked(limit, pageCursor, likeType, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listPersonLiked")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listPersonLiked")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **likeType** | [**LikeType**](.md)|  | [optional] [enum: all, liked_only, disliked_only] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**PersonContentType**](.md)|  | [optional] [enum: all, comments, posts] |

### Return type

[**PagedResponsePostCommentCombinedView**](PagedResponsePostCommentCombinedView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersonRead"></a>
# **listPersonRead**
> PagedResponsePostView listPersonRead(limit, pageCursor)

List your read content.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponsePostView = apiInstance.listPersonRead(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listPersonRead")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listPersonRead")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponsePostView**](PagedResponsePostView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersonSaved"></a>
# **listPersonSaved**
> PagedResponsePostCommentCombinedView listPersonSaved(limit, pageCursor, searchTerm, type)

List your saved content.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val type : PersonContentType =  // PersonContentType | 
try {
    val result : PagedResponsePostCommentCombinedView = apiInstance.listPersonSaved(limit, pageCursor, searchTerm, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listPersonSaved")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listPersonSaved")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **searchTerm** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**PersonContentType**](.md)|  | [optional] [enum: all, comments, posts] |

### Return type

[**PagedResponsePostCommentCombinedView**](PagedResponsePostCommentCombinedView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listRegistrationInvitations"></a>
# **listRegistrationInvitations**
> PagedResponseLocalUserInvite listRegistrationInvitations(limit, pageCursor)

Revoke a previously created registration invitation

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseLocalUserInvite = apiInstance.listRegistrationInvitations(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#listRegistrationInvitations")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#listRegistrationInvitations")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponseLocalUserInvite**](PagedResponseLocalUserInvite.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="login"></a>
# **login**
> LoginResponse login(login)

Log into lemmy.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val login : Login =  // Login | 
try {
    val result : LoginResponse = apiInstance.login(login)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#login")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#login")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **login** | [**Login**](Login.md)|  | |

### Return type

[**LoginResponse**](LoginResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="logout"></a>
# **logout**
> SuccessResponse logout()

Invalidate the currently used auth token.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.logout()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#logout")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#logout")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="markAllNotificationsAsRead"></a>
# **markAllNotificationsAsRead**
> SuccessResponse markAllNotificationsAsRead()

Mark all notifications as read.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.markAllNotificationsAsRead()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#markAllNotificationsAsRead")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#markAllNotificationsAsRead")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="markDonationDialogShown"></a>
# **markDonationDialogShown**
> SuccessResponse markDonationDialogShown()



Mark the donation dialog as shown, so it isn&#39;t displayed anymore.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.markDonationDialogShown()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#markDonationDialogShown")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#markDonationDialogShown")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="markNotificationAsRead"></a>
# **markNotificationAsRead**
> SuccessResponse markNotificationAsRead(markNotificationAsRead)

Mark a notification as read.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val markNotificationAsRead : MarkNotificationAsRead =  // MarkNotificationAsRead | 
try {
    val result : SuccessResponse = apiInstance.markNotificationAsRead(markNotificationAsRead)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#markNotificationAsRead")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#markNotificationAsRead")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **markNotificationAsRead** | [**MarkNotificationAsRead**](MarkNotificationAsRead.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="register"></a>
# **register**
> LoginResponse register(register)

Register a new user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val register : Register =  // Register | 
try {
    val result : LoginResponse = apiInstance.register(register)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#register")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#register")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **register** | [**Register**](Register.md)|  | |

### Return type

[**LoginResponse**](LoginResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resendVerificationEmail"></a>
# **resendVerificationEmail**
> SuccessResponse resendVerificationEmail(resendVerificationEmail)

Resend a verification email.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val resendVerificationEmail : ResendVerificationEmail =  // ResendVerificationEmail | 
try {
    val result : SuccessResponse = apiInstance.resendVerificationEmail(resendVerificationEmail)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#resendVerificationEmail")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#resendVerificationEmail")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resendVerificationEmail** | [**ResendVerificationEmail**](ResendVerificationEmail.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resetPassword"></a>
# **resetPassword**
> SuccessResponse resetPassword(resetPassword)

Reset your password.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val resetPassword : ResetPassword =  // ResetPassword | 
try {
    val result : SuccessResponse = apiInstance.resetPassword(resetPassword)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#resetPassword")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#resetPassword")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resetPassword** | [**ResetPassword**](ResetPassword.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="revokeRegistrationInvitation"></a>
# **revokeRegistrationInvitation**
> SuccessResponse revokeRegistrationInvitation(revokeInvitation)

Revoke a previously created registration invitation

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val revokeInvitation : RevokeInvitation =  // RevokeInvitation | 
try {
    val result : SuccessResponse = apiInstance.revokeRegistrationInvitation(revokeInvitation)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#revokeRegistrationInvitation")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#revokeRegistrationInvitation")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **revokeInvitation** | [**RevokeInvitation**](RevokeInvitation.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="saveUserSettings"></a>
# **saveUserSettings**
> SuccessResponse saveUserSettings(saveUserSettings)

Save your user settings.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val saveUserSettings : SaveUserSettings =  // SaveUserSettings | 
try {
    val result : SuccessResponse = apiInstance.saveUserSettings(saveUserSettings)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#saveUserSettings")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#saveUserSettings")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **saveUserSettings** | [**SaveUserSettings**](SaveUserSettings.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="uploadUserAvatar"></a>
# **uploadUserAvatar**
> UploadImageResponse uploadUserAvatar(image)

Upload new user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadUserAvatar(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#uploadUserAvatar")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#uploadUserAvatar")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **image** | **java.io.File**|  | |

### Return type

[**UploadImageResponse**](UploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadUserBanner"></a>
# **uploadUserBanner**
> UploadImageResponse uploadUserBanner(image)

Upload new user banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadUserBanner(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#uploadUserBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#uploadUserBanner")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **image** | **java.io.File**|  | |

### Return type

[**UploadImageResponse**](UploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="userBlockInstanceCommunities"></a>
# **userBlockInstanceCommunities**
> SuccessResponse userBlockInstanceCommunities(userBlockInstanceCommunitiesParams)

Block an instance&#39;s communities as a user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val userBlockInstanceCommunitiesParams : UserBlockInstanceCommunitiesParams =  // UserBlockInstanceCommunitiesParams | 
try {
    val result : SuccessResponse = apiInstance.userBlockInstanceCommunities(userBlockInstanceCommunitiesParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#userBlockInstanceCommunities")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#userBlockInstanceCommunities")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userBlockInstanceCommunitiesParams** | [**UserBlockInstanceCommunitiesParams**](UserBlockInstanceCommunitiesParams.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="userBlockInstancePersons"></a>
# **userBlockInstancePersons**
> SuccessResponse userBlockInstancePersons(userBlockInstancePersonsParams)

Block an instance&#39;s persons as a user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val userBlockInstancePersonsParams : UserBlockInstancePersonsParams =  // UserBlockInstancePersonsParams | 
try {
    val result : SuccessResponse = apiInstance.userBlockInstancePersons(userBlockInstancePersonsParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#userBlockInstancePersons")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#userBlockInstancePersons")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userBlockInstancePersonsParams** | [**UserBlockInstancePersonsParams**](UserBlockInstancePersonsParams.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="validateAuth"></a>
# **validateAuth**
> SuccessResponse validateAuth()

Returns an error message if your auth token is invalid

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
try {
    val result : SuccessResponse = apiInstance.validateAuth()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#validateAuth")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#validateAuth")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="verifyEmail"></a>
# **verifyEmail**
> SuccessResponse verifyEmail(verifyEmail)

Verify your email

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AccountApi()
val verifyEmail : VerifyEmail =  // VerifyEmail | 
try {
    val result : SuccessResponse = apiInstance.verifyEmail(verifyEmail)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AccountApi#verifyEmail")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AccountApi#verifyEmail")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **verifyEmail** | [**VerifyEmail**](VerifyEmail.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

