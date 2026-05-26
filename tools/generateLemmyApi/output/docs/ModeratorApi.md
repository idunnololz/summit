# ModeratorApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**addModToCommunity**](ModeratorApi.md#addModToCommunity) | **POST** /api/v4/community/mod | Add a moderator to your community. |
| [**banFromCommunity**](ModeratorApi.md#banFromCommunity) | **POST** /api/v4/community/ban_user | Ban a user from a community. |
| [**distinguishComment**](ModeratorApi.md#distinguishComment) | **POST** /api/v4/comment/distinguish | Distinguishes a comment (speak as moderator) |
| [**featurePost**](ModeratorApi.md#featurePost) | **POST** /api/v4/post/feature | A moderator can feature a community post ( IE stick it to the top of a community ). |
| [**lockComment**](ModeratorApi.md#lockComment) | **POST** /api/v4/comment/lock | A moderator can lock a comment (IE disable replies). |
| [**removeComment**](ModeratorApi.md#removeComment) | **POST** /api/v4/comment/remove | A moderator remove for a comment. |
| [**removeCommunity**](ModeratorApi.md#removeCommunity) | **POST** /api/v4/community/remove | A moderator remove for a community. |
| [**removePost**](ModeratorApi.md#removePost) | **POST** /api/v4/post/remove | A moderator remove for a post. |
| [**resolveCommentReport**](ModeratorApi.md#resolveCommentReport) | **PUT** /api/v4/comment/report/resolve | Resolve a comment report. Only a mod can do this. |
| [**resolvePostReport**](ModeratorApi.md#resolvePostReport) | **PUT** /api/v4/post/report/resolve | Resolve a post report. Only a mod can do this. |
| [**transferCommunity**](ModeratorApi.md#transferCommunity) | **POST** /api/v4/community/transfer | Transfer your community to an existing moderator. |


<a id="addModToCommunity"></a>
# **addModToCommunity**
> RequestStateAddModToCommunityResponse addModToCommunity(addModToCommunity)

Add a moderator to your community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val addModToCommunity : AddModToCommunity =  // AddModToCommunity | 
try {
    val result : RequestStateAddModToCommunityResponse = apiInstance.addModToCommunity(addModToCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#addModToCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#addModToCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **addModToCommunity** | [**AddModToCommunity**](AddModToCommunity.md)|  | |

### Return type

[**RequestStateAddModToCommunityResponse**](RequestStateAddModToCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="banFromCommunity"></a>
# **banFromCommunity**
> RequestStatePersonResponse banFromCommunity(banFromCommunity)

Ban a user from a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val banFromCommunity : BanFromCommunity =  // BanFromCommunity | 
try {
    val result : RequestStatePersonResponse = apiInstance.banFromCommunity(banFromCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#banFromCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#banFromCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **banFromCommunity** | [**BanFromCommunity**](BanFromCommunity.md)|  | |

### Return type

[**RequestStatePersonResponse**](RequestStatePersonResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="distinguishComment"></a>
# **distinguishComment**
> RequestStateCommentResponse distinguishComment(distinguishComment)

Distinguishes a comment (speak as moderator)

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val distinguishComment : DistinguishComment =  // DistinguishComment | 
try {
    val result : RequestStateCommentResponse = apiInstance.distinguishComment(distinguishComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#distinguishComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#distinguishComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **distinguishComment** | [**DistinguishComment**](DistinguishComment.md)|  | |

### Return type

[**RequestStateCommentResponse**](RequestStateCommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="featurePost"></a>
# **featurePost**
> RequestStatePostResponse featurePost(featurePost)

A moderator can feature a community post ( IE stick it to the top of a community ).

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val featurePost : FeaturePost =  // FeaturePost | 
try {
    val result : RequestStatePostResponse = apiInstance.featurePost(featurePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#featurePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#featurePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **featurePost** | [**FeaturePost**](FeaturePost.md)|  | |

### Return type

[**RequestStatePostResponse**](RequestStatePostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="lockComment"></a>
# **lockComment**
> RequestStateCommentResponse lockComment(lockComment)

A moderator can lock a comment (IE disable replies).

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val lockComment : LockComment =  // LockComment | 
try {
    val result : RequestStateCommentResponse = apiInstance.lockComment(lockComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#lockComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#lockComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **lockComment** | [**LockComment**](LockComment.md)|  | |

### Return type

[**RequestStateCommentResponse**](RequestStateCommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="removeComment"></a>
# **removeComment**
> RequestStateCommentResponse removeComment(removeComment)

A moderator remove for a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val removeComment : RemoveComment =  // RemoveComment | 
try {
    val result : RequestStateCommentResponse = apiInstance.removeComment(removeComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#removeComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#removeComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removeComment** | [**RemoveComment**](RemoveComment.md)|  | |

### Return type

[**RequestStateCommentResponse**](RequestStateCommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="removeCommunity"></a>
# **removeCommunity**
> RequestStateCommunityResponse removeCommunity(removeCommunity)

A moderator remove for a community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val removeCommunity : RemoveCommunity =  // RemoveCommunity | 
try {
    val result : RequestStateCommunityResponse = apiInstance.removeCommunity(removeCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#removeCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#removeCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removeCommunity** | [**RemoveCommunity**](RemoveCommunity.md)|  | |

### Return type

[**RequestStateCommunityResponse**](RequestStateCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="removePost"></a>
# **removePost**
> RequestStatePostResponse removePost(removePost)

A moderator remove for a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val removePost : RemovePost =  // RemovePost | 
try {
    val result : RequestStatePostResponse = apiInstance.removePost(removePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#removePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#removePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removePost** | [**RemovePost**](RemovePost.md)|  | |

### Return type

[**RequestStatePostResponse**](RequestStatePostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolveCommentReport"></a>
# **resolveCommentReport**
> RequestStateCommentReportResponse resolveCommentReport(resolveCommentReport)

Resolve a comment report. Only a mod can do this.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val resolveCommentReport : ResolveCommentReport =  // ResolveCommentReport | 
try {
    val result : RequestStateCommentReportResponse = apiInstance.resolveCommentReport(resolveCommentReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#resolveCommentReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#resolveCommentReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolveCommentReport** | [**ResolveCommentReport**](ResolveCommentReport.md)|  | |

### Return type

[**RequestStateCommentReportResponse**](RequestStateCommentReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolvePostReport"></a>
# **resolvePostReport**
> RequestStatePostReportResponse resolvePostReport(resolvePostReport)

Resolve a post report. Only a mod can do this.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val resolvePostReport : ResolvePostReport =  // ResolvePostReport | 
try {
    val result : RequestStatePostReportResponse = apiInstance.resolvePostReport(resolvePostReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#resolvePostReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#resolvePostReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolvePostReport** | [**ResolvePostReport**](ResolvePostReport.md)|  | |

### Return type

[**RequestStatePostReportResponse**](RequestStatePostReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="transferCommunity"></a>
# **transferCommunity**
> RequestStateGetCommunityResponse transferCommunity(transferCommunity)

Transfer your community to an existing moderator.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = ModeratorApi()
val transferCommunity : TransferCommunity =  // TransferCommunity | 
try {
    val result : RequestStateGetCommunityResponse = apiInstance.transferCommunity(transferCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling ModeratorApi#transferCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling ModeratorApi#transferCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **transferCommunity** | [**TransferCommunity**](TransferCommunity.md)|  | |

### Return type

[**RequestStateGetCommunityResponse**](RequestStateGetCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

