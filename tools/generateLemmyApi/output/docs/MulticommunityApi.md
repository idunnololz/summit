# MulticommunityApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createMultiCommunity**](MulticommunityApi.md#createMultiCommunity) | **POST** /api/v4/multi_community |  |
| [**createMultiCommunityEntry**](MulticommunityApi.md#createMultiCommunityEntry) | **POST** /api/v4/multi_community/entry |  |
| [**deleteMultiCommunityEntry**](MulticommunityApi.md#deleteMultiCommunityEntry) | **DELETE** /api/v4/multi_community/entry |  |
| [**editMultiCommunity**](MulticommunityApi.md#editMultiCommunity) | **PUT** /api/v4/multi_community |  |
| [**followMultiCommunity**](MulticommunityApi.md#followMultiCommunity) | **POST** /api/v4/multi_community/follow |  |
| [**getMultiCommunity**](MulticommunityApi.md#getMultiCommunity) | **GET** /api/v4/multi_community |  |
| [**listMultiCommunities**](MulticommunityApi.md#listMultiCommunities) | **GET** /api/v4/multi_community/list |  |


<a id="createMultiCommunity"></a>
# **createMultiCommunity**
> MultiCommunityResponse createMultiCommunity(createMultiCommunity)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val createMultiCommunity : CreateMultiCommunity =  // CreateMultiCommunity | 
try {
    val result : MultiCommunityResponse = apiInstance.createMultiCommunity(createMultiCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#createMultiCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#createMultiCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createMultiCommunity** | [**CreateMultiCommunity**](CreateMultiCommunity.md)|  | |

### Return type

[**MultiCommunityResponse**](MultiCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createMultiCommunityEntry"></a>
# **createMultiCommunityEntry**
> CommunityResponse createMultiCommunityEntry(createOrDeleteMultiCommunityEntry)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val createOrDeleteMultiCommunityEntry : CreateOrDeleteMultiCommunityEntry =  // CreateOrDeleteMultiCommunityEntry | 
try {
    val result : CommunityResponse = apiInstance.createMultiCommunityEntry(createOrDeleteMultiCommunityEntry)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#createMultiCommunityEntry")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#createMultiCommunityEntry")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createOrDeleteMultiCommunityEntry** | [**CreateOrDeleteMultiCommunityEntry**](CreateOrDeleteMultiCommunityEntry.md)|  | |

### Return type

[**CommunityResponse**](CommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteMultiCommunityEntry"></a>
# **deleteMultiCommunityEntry**
> SuccessResponse deleteMultiCommunityEntry(createOrDeleteMultiCommunityEntry)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val createOrDeleteMultiCommunityEntry : CreateOrDeleteMultiCommunityEntry =  // CreateOrDeleteMultiCommunityEntry | 
try {
    val result : SuccessResponse = apiInstance.deleteMultiCommunityEntry(createOrDeleteMultiCommunityEntry)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#deleteMultiCommunityEntry")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#deleteMultiCommunityEntry")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createOrDeleteMultiCommunityEntry** | [**CreateOrDeleteMultiCommunityEntry**](CreateOrDeleteMultiCommunityEntry.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editMultiCommunity"></a>
# **editMultiCommunity**
> MultiCommunityResponse editMultiCommunity(editMultiCommunity)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val editMultiCommunity : EditMultiCommunity =  // EditMultiCommunity | 
try {
    val result : MultiCommunityResponse = apiInstance.editMultiCommunity(editMultiCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#editMultiCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#editMultiCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editMultiCommunity** | [**EditMultiCommunity**](EditMultiCommunity.md)|  | |

### Return type

[**MultiCommunityResponse**](MultiCommunityResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="followMultiCommunity"></a>
# **followMultiCommunity**
> MultiCommunityResponse followMultiCommunity(followMultiCommunity)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val followMultiCommunity : FollowMultiCommunity =  // FollowMultiCommunity | 
try {
    val result : MultiCommunityResponse = apiInstance.followMultiCommunity(followMultiCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#followMultiCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#followMultiCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **followMultiCommunity** | [**FollowMultiCommunity**](FollowMultiCommunity.md)|  | |

### Return type

[**MultiCommunityResponse**](MultiCommunityResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getMultiCommunity"></a>
# **getMultiCommunity**
> GetMultiCommunityResponse getMultiCommunity(name, id)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val name : kotlin.String = name_example // kotlin.String | 
val id : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : GetMultiCommunityResponse = apiInstance.getMultiCommunity(name, id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#getMultiCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#getMultiCommunity")
    e.printStackTrace()
}
```

### Parameters
| **name** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Double**|  | [optional] |

### Return type

[**GetMultiCommunityResponse**](GetMultiCommunityResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listMultiCommunities"></a>
# **listMultiCommunities**
> PagedResponseMultiCommunityView listMultiCommunities(limit, pageCursor, searchTitleOnly, searchTerm, timeRangeSeconds, creatorId, sort, type)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MulticommunityApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val searchTitleOnly : kotlin.Boolean = true // kotlin.Boolean | 
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute.
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val sort : MultiCommunitySortType =  // MultiCommunitySortType | 
val type : MultiCommunityListingType =  // MultiCommunityListingType | 
try {
    val result : PagedResponseMultiCommunityView = apiInstance.listMultiCommunities(limit, pageCursor, searchTitleOnly, searchTerm, timeRangeSeconds, creatorId, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MulticommunityApi#listMultiCommunities")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MulticommunityApi#listMultiCommunities")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **searchTitleOnly** | **kotlin.Boolean**|  | [optional] |
| **searchTerm** | **kotlin.String**|  | [optional] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **sort** | [**MultiCommunitySortType**](.md)|  | [optional] [enum: new, old, name_asc, name_desc, communities, subscribers, subscribers_local] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**MultiCommunityListingType**](.md)|  | [optional] [enum: all, local, subscribed] |

### Return type

[**PagedResponseMultiCommunityView**](PagedResponseMultiCommunityView.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

