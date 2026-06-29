# CommunityApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**addModToCommunity**](CommunityApi.md#addModToCommunity) | **POST** /api/v4/community/mod | Add a moderator to your community. |
| [**approveCommunityPendingFollow**](CommunityApi.md#approveCommunityPendingFollow) | **POST** /api/v4/community/pending_follows/approve | Approve a community pending follow request. |
| [**banFromCommunity**](CommunityApi.md#banFromCommunity) | **POST** /api/v4/community/ban_user | Ban a user from a community. |
| [**blockCommunity**](CommunityApi.md#blockCommunity) | **POST** /api/v4/account/block/community | Block a community. |
| [**createCommunity**](CommunityApi.md#createCommunity) | **POST** /api/v4/community | Create a new community. |
| [**createCommunityReport**](CommunityApi.md#createCommunityReport) | **POST** /api/v4/community/report | Create a report for a community. |
| [**createCommunityTag**](CommunityApi.md#createCommunityTag) | **POST** /api/v4/community/tag | Create a community post tag. |
| [**deleteCommunity**](CommunityApi.md#deleteCommunity) | **DELETE** /api/v4/community | Delete a community. |
| [**deleteCommunityBanner**](CommunityApi.md#deleteCommunityBanner) | **DELETE** /api/v4/community/banner | Delete the community banner. |
| [**deleteCommunityIcon**](CommunityApi.md#deleteCommunityIcon) | **DELETE** /api/v4/community/icon | Delete the community icon. |
| [**deleteCommunityTag**](CommunityApi.md#deleteCommunityTag) | **DELETE** /api/v4/community/tag | Delete a post tag in a community. |
| [**editCommunity**](CommunityApi.md#editCommunity) | **PUT** /api/v4/community | Edit a community. |
| [**editCommunityNotifications**](CommunityApi.md#editCommunityNotifications) | **PUT** /api/v4/community/notifications | Change notification settings for a community |
| [**editCommunityTag**](CommunityApi.md#editCommunityTag) | **PUT** /api/v4/community/tag | Edit a community post tag. |
| [**followCommunity**](CommunityApi.md#followCommunity) | **POST** /api/v4/community/follow | Follow / subscribe to a community. |
| [**getCommunity**](CommunityApi.md#getCommunity) | **GET** /api/v4/community | Get / fetch a community. |
| [**getRandomCommunity**](CommunityApi.md#getRandomCommunity) | **GET** /api/v4/community/random | Get a random community. |
| [**hideCommunity**](CommunityApi.md#hideCommunity) | **PUT** /api/v4/community/hide | Hide a community from public / \&quot;All\&quot; view. Admins only. |
| [**listCommunities**](CommunityApi.md#listCommunities) | **GET** /api/v4/community/list | List communities, with various filters. |
| [**listCommunityPendingFollows**](CommunityApi.md#listCommunityPendingFollows) | **GET** /api/v4/community/pending_follows/list | Get a community&#39;s pending followers. |
| [**removeCommunity**](CommunityApi.md#removeCommunity) | **POST** /api/v4/community/remove | A moderator remove for a community. |
| [**resolveCommunityReport**](CommunityApi.md#resolveCommunityReport) | **PUT** /api/v4/community/report/resolve | Resolve a report for a private message. |
| [**transferCommunity**](CommunityApi.md#transferCommunity) | **POST** /api/v4/community/transfer | Transfer your community to an existing moderator. |
| [**uploadCommunityBanner**](CommunityApi.md#uploadCommunityBanner) | **POST** /api/v4/community/banner | Upload new community banner. |
| [**uploadCommunityIcon**](CommunityApi.md#uploadCommunityIcon) | **POST** /api/v4/community/icon | Upload new community icon. |
| [**userBlockInstanceCommunities**](CommunityApi.md#userBlockInstanceCommunities) | **POST** /api/v4/account/block/instance/communities | Block an instance&#39;s communities as a user. |


<a id="addModToCommunity"></a>
# **addModToCommunity**
> AddModToCommunityResponse addModToCommunity(addModToCommunity)

Add a moderator to your community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val addModToCommunity : AddModToCommunity =  // AddModToCommunity | 
try {
    val result : AddModToCommunityResponse = apiInstance.addModToCommunity(addModToCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#addModToCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#addModToCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **addModToCommunity** | [**AddModToCommunity**](AddModToCommunity.md)|  | |

### Return type

[**AddModToCommunityResponse**](AddModToCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="approveCommunityPendingFollow"></a>
# **approveCommunityPendingFollow**
> SuccessResponse approveCommunityPendingFollow(approveCommunityPendingFollower)

Approve a community pending follow request.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val approveCommunityPendingFollower : ApproveCommunityPendingFollower =  // ApproveCommunityPendingFollower | 
try {
    val result : SuccessResponse = apiInstance.approveCommunityPendingFollow(approveCommunityPendingFollower)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#approveCommunityPendingFollow")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#approveCommunityPendingFollow")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **approveCommunityPendingFollower** | [**ApproveCommunityPendingFollower**](ApproveCommunityPendingFollower.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="banFromCommunity"></a>
# **banFromCommunity**
> PersonResponse banFromCommunity(banFromCommunity)

Ban a user from a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val banFromCommunity : BanFromCommunity =  // BanFromCommunity | 
try {
    val result : PersonResponse = apiInstance.banFromCommunity(banFromCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#banFromCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#banFromCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **banFromCommunity** | [**BanFromCommunity**](BanFromCommunity.md)|  | |

### Return type

[**PersonResponse**](PersonResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="blockCommunity"></a>
# **blockCommunity**
> CommunityResponse blockCommunity(blockCommunity)

Block a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val blockCommunity : BlockCommunity =  // BlockCommunity | 
try {
    val result : CommunityResponse = apiInstance.blockCommunity(blockCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#blockCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#blockCommunity")
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

<a id="createCommunity"></a>
# **createCommunity**
> CommunityResponse createCommunity(createCommunity)

Create a new community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val createCommunity : CreateCommunity =  // CreateCommunity | 
try {
    val result : CommunityResponse = apiInstance.createCommunity(createCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#createCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#createCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommunity** | [**CreateCommunity**](CreateCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createCommunityReport"></a>
# **createCommunityReport**
> CommunityReportResponse createCommunityReport(createCommunityReport)

Create a report for a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val createCommunityReport : CreateCommunityReport =  // CreateCommunityReport | 
try {
    val result : CommunityReportResponse = apiInstance.createCommunityReport(createCommunityReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#createCommunityReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#createCommunityReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommunityReport** | [**CreateCommunityReport**](CreateCommunityReport.md)|  | |

### Return type

[**CommunityReportResponse**](CommunityReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createCommunityTag"></a>
# **createCommunityTag**
> CommunityTag createCommunityTag(createCommunityTag)

Create a community post tag.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val createCommunityTag : CreateCommunityTag =  // CreateCommunityTag | 
try {
    val result : CommunityTag = apiInstance.createCommunityTag(createCommunityTag)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#createCommunityTag")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#createCommunityTag")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommunityTag** | [**CreateCommunityTag**](CreateCommunityTag.md)|  | |

### Return type

[**CommunityTag**](CommunityTag.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCommunity"></a>
# **deleteCommunity**
> CommunityResponse deleteCommunity(deleteCommunity)

Delete a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val deleteCommunity : DeleteCommunity =  // DeleteCommunity | 
try {
    val result : CommunityResponse = apiInstance.deleteCommunity(deleteCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#deleteCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#deleteCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteCommunity** | [**DeleteCommunity**](DeleteCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCommunityBanner"></a>
# **deleteCommunityBanner**
> SuccessResponse deleteCommunityBanner(communityIdQuery)

Delete the community banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : SuccessResponse = apiInstance.deleteCommunityBanner(communityIdQuery)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#deleteCommunityBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#deleteCommunityBanner")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **communityIdQuery** | [**CommunityIdQuery**](CommunityIdQuery.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCommunityIcon"></a>
# **deleteCommunityIcon**
> SuccessResponse deleteCommunityIcon(communityIdQuery)

Delete the community icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : SuccessResponse = apiInstance.deleteCommunityIcon(communityIdQuery)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#deleteCommunityIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#deleteCommunityIcon")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **communityIdQuery** | [**CommunityIdQuery**](CommunityIdQuery.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCommunityTag"></a>
# **deleteCommunityTag**
> CommunityTag deleteCommunityTag(deleteCommunityTag)

Delete a post tag in a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val deleteCommunityTag : DeleteCommunityTag =  // DeleteCommunityTag | 
try {
    val result : CommunityTag = apiInstance.deleteCommunityTag(deleteCommunityTag)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#deleteCommunityTag")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#deleteCommunityTag")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteCommunityTag** | [**DeleteCommunityTag**](DeleteCommunityTag.md)|  | |

### Return type

[**CommunityTag**](CommunityTag.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editCommunity"></a>
# **editCommunity**
> CommunityResponse editCommunity(editCommunity)

Edit a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val editCommunity : EditCommunity =  // EditCommunity | 
try {
    val result : CommunityResponse = apiInstance.editCommunity(editCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#editCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#editCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editCommunity** | [**EditCommunity**](EditCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editCommunityNotifications"></a>
# **editCommunityNotifications**
> SuccessResponse editCommunityNotifications(editCommunityNotifications)

Change notification settings for a community

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val editCommunityNotifications : EditCommunityNotifications =  // EditCommunityNotifications | 
try {
    val result : SuccessResponse = apiInstance.editCommunityNotifications(editCommunityNotifications)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#editCommunityNotifications")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#editCommunityNotifications")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editCommunityNotifications** | [**EditCommunityNotifications**](EditCommunityNotifications.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editCommunityTag"></a>
# **editCommunityTag**
> CommunityTag editCommunityTag(editCommunityTag)

Edit a community post tag.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val editCommunityTag : EditCommunityTag =  // EditCommunityTag | 
try {
    val result : CommunityTag = apiInstance.editCommunityTag(editCommunityTag)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#editCommunityTag")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#editCommunityTag")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editCommunityTag** | [**EditCommunityTag**](EditCommunityTag.md)|  | |

### Return type

[**CommunityTag**](CommunityTag.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="followCommunity"></a>
# **followCommunity**
> CommunityResponse followCommunity(followCommunity)

Follow / subscribe to a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val followCommunity : FollowCommunity =  // FollowCommunity | 
try {
    val result : CommunityResponse = apiInstance.followCommunity(followCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#followCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#followCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **followCommunity** | [**FollowCommunity**](FollowCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getCommunity"></a>
# **getCommunity**
> GetCommunityResponse getCommunity(name, id)

Get / fetch a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val name : kotlin.String = name_example // kotlin.String | Example: star_trek , or star_trek@xyz.tld
val id : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : GetCommunityResponse = apiInstance.getCommunity(name, id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#getCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#getCommunity")
    e.printStackTrace()
}
```

### Parameters
| **name** | **kotlin.String**| Example: star_trek , or star_trek@xyz.tld | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Double**|  | [optional] |

### Return type

[**GetCommunityResponse**](GetCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getRandomCommunity"></a>
# **getRandomCommunity**
> CommunityResponse getRandomCommunity(showNsfw, type)

Get a random community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val showNsfw : kotlin.Boolean = true // kotlin.Boolean | 
val type : ListingType =  // ListingType | 
try {
    val result : CommunityResponse = apiInstance.getRandomCommunity(showNsfw, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#getRandomCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#getRandomCommunity")
    e.printStackTrace()
}
```

### Parameters
| **showNsfw** | **kotlin.Boolean**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="hideCommunity"></a>
# **hideCommunity**
> SuccessResponse hideCommunity(hideCommunity)

Hide a community from public / \&quot;All\&quot; view. Admins only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val hideCommunity : HideCommunity =  // HideCommunity | 
try {
    val result : SuccessResponse = apiInstance.hideCommunity(hideCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#hideCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#hideCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **hideCommunity** | [**HideCommunity**](HideCommunity.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listCommunities"></a>
# **listCommunities**
> PagedResponseCommunityView listCommunities(limit, pageCursor, searchTitleOnly, searchTerm, multiCommunityId, showNsfw, timeRangeSeconds, sort, type)

List communities, with various filters.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val searchTitleOnly : kotlin.Boolean = true // kotlin.Boolean | 
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val multiCommunityId : kotlin.Double = 1.2 // kotlin.Double | 
val showNsfw : kotlin.Boolean = true // kotlin.Boolean | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute.
val sort : CommunitySortType =  // CommunitySortType | 
val type : ListingType =  // ListingType | 
try {
    val result : PagedResponseCommunityView = apiInstance.listCommunities(limit, pageCursor, searchTitleOnly, searchTerm, multiCommunityId, showNsfw, timeRangeSeconds, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#listCommunities")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#listCommunities")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **searchTitleOnly** | **kotlin.Boolean**|  | [optional] |
| **searchTerm** | **kotlin.String**|  | [optional] |
| **multiCommunityId** | **kotlin.Double**|  | [optional] |
| **showNsfw** | **kotlin.Boolean**|  | [optional] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. | [optional] |
| **sort** | [**CommunitySortType**](.md)|  | [optional] [enum: active_six_months, active_monthly, active_weekly, active_daily, hot, new, old, name_asc, name_desc, comments, posts, subscribers, subscribers_local] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |

### Return type

[**PagedResponseCommunityView**](PagedResponseCommunityView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listCommunityPendingFollows"></a>
# **listCommunityPendingFollows**
> PagedResponsePendingFollowerView listCommunityPendingFollows(limit, pageCursor, allCommunities, unreadOnly)

Get a community&#39;s pending followers.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val allCommunities : kotlin.Boolean = true // kotlin.Boolean | 
val unreadOnly : kotlin.Boolean = true // kotlin.Boolean | Only shows the unapproved applications
try {
    val result : PagedResponsePendingFollowerView = apiInstance.listCommunityPendingFollows(limit, pageCursor, allCommunities, unreadOnly)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#listCommunityPendingFollows")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#listCommunityPendingFollows")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **allCommunities** | **kotlin.Boolean**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **unreadOnly** | **kotlin.Boolean**| Only shows the unapproved applications | [optional] |

### Return type

[**PagedResponsePendingFollowerView**](PagedResponsePendingFollowerView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="removeCommunity"></a>
# **removeCommunity**
> CommunityResponse removeCommunity(removeCommunity)

A moderator remove for a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val removeCommunity : RemoveCommunity =  // RemoveCommunity | 
try {
    val result : CommunityResponse = apiInstance.removeCommunity(removeCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#removeCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#removeCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removeCommunity** | [**RemoveCommunity**](RemoveCommunity.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolveCommunityReport"></a>
# **resolveCommunityReport**
> CommunityReportResponse resolveCommunityReport(resolveCommunityReport)

Resolve a report for a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val resolveCommunityReport : ResolveCommunityReport =  // ResolveCommunityReport | 
try {
    val result : CommunityReportResponse = apiInstance.resolveCommunityReport(resolveCommunityReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#resolveCommunityReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#resolveCommunityReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolveCommunityReport** | [**ResolveCommunityReport**](ResolveCommunityReport.md)|  | |

### Return type

[**CommunityReportResponse**](CommunityReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="transferCommunity"></a>
# **transferCommunity**
> GetCommunityResponse transferCommunity(transferCommunity)

Transfer your community to an existing moderator.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val transferCommunity : TransferCommunity =  // TransferCommunity | 
try {
    val result : GetCommunityResponse = apiInstance.transferCommunity(transferCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#transferCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#transferCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **transferCommunity** | [**TransferCommunity**](TransferCommunity.md)|  | |

### Return type

[**GetCommunityResponse**](GetCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="uploadCommunityBanner"></a>
# **uploadCommunityBanner**
> UploadImageResponse uploadCommunityBanner(id, image)

Upload new community banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val id : kotlin.Double = 1.2 // kotlin.Double | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadCommunityBanner(id, image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#uploadCommunityBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#uploadCommunityBanner")
    e.printStackTrace()
}
```

### Parameters
| **id** | **kotlin.Double**|  | |
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

<a id="uploadCommunityIcon"></a>
# **uploadCommunityIcon**
> UploadImageResponse uploadCommunityIcon(id, image)

Upload new community icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommunityApi()
val id : kotlin.Double = 1.2 // kotlin.Double | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadCommunityIcon(id, image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#uploadCommunityIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#uploadCommunityIcon")
    e.printStackTrace()
}
```

### Parameters
| **id** | **kotlin.Double**|  | |
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

val apiInstance = CommunityApi()
val userBlockInstanceCommunitiesParams : UserBlockInstanceCommunitiesParams =  // UserBlockInstanceCommunitiesParams | 
try {
    val result : SuccessResponse = apiInstance.userBlockInstanceCommunities(userBlockInstanceCommunitiesParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommunityApi#userBlockInstanceCommunities")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommunityApi#userBlockInstanceCommunities")
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

