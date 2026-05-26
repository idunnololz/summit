# MiscellaneousApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**authenticateWithOAuth**](MiscellaneousApi.md#authenticateWithOAuth) | **POST** /api/v4/oauth/authenticate | Authenticate with OAuth |
| [**createOAuthProvider**](MiscellaneousApi.md#createOAuthProvider) | **POST** /api/v4/oauth_provider | Create a new oauth provider method |
| [**deleteOAuthProvider**](MiscellaneousApi.md#deleteOAuthProvider) | **DELETE** /api/v4/oauth_provider | Delete an oauth provider method |
| [**editOAuthProvider**](MiscellaneousApi.md#editOAuthProvider) | **PUT** /api/v4/oauth_provider | Edit an existing oauth provider method |
| [**getFederatedInstances**](MiscellaneousApi.md#getFederatedInstances) | **GET** /api/v4/federated_instances | Fetch federated instances. |
| [**getModlog**](MiscellaneousApi.md#getModlog) | **GET** /api/v4/modlog | Get the modlog. |
| [**getSiteMetadata**](MiscellaneousApi.md#getSiteMetadata) | **GET** /api/v4/post/site_metadata | Fetch metadata for any given site. |
| [**listUsers**](MiscellaneousApi.md#listUsers) | **GET** /api/v4/admin/users | Get a list of users. |
| [**resolveObject**](MiscellaneousApi.md#resolveObject) | **GET** /api/v4/resolve_object | Fetch a non-local / federated object. |
| [**search**](MiscellaneousApi.md#search) | **GET** /api/v4/search | Search lemmy. If &#x60;search_term&#x60; is a url it also attempts to fetch it, just like &#x60;resolve_object&#x60;. |


<a id="authenticateWithOAuth"></a>
# **authenticateWithOAuth**
> RequestStateLoginResponse authenticateWithOAuth(authenticateWithOauth)

Authenticate with OAuth

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val authenticateWithOauth : AuthenticateWithOauth =  // AuthenticateWithOauth | 
try {
    val result : RequestStateLoginResponse = apiInstance.authenticateWithOAuth(authenticateWithOauth)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#authenticateWithOAuth")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#authenticateWithOAuth")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **authenticateWithOauth** | [**AuthenticateWithOauth**](AuthenticateWithOauth.md)|  | |

### Return type

[**RequestStateLoginResponse**](RequestStateLoginResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createOAuthProvider"></a>
# **createOAuthProvider**
> RequestStateAdminOAuthProvider createOAuthProvider(createOAuthProvider)

Create a new oauth provider method

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val createOAuthProvider : CreateOAuthProvider =  // CreateOAuthProvider | 
try {
    val result : RequestStateAdminOAuthProvider = apiInstance.createOAuthProvider(createOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#createOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#createOAuthProvider")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createOAuthProvider** | [**CreateOAuthProvider**](CreateOAuthProvider.md)|  | |

### Return type

[**RequestStateAdminOAuthProvider**](RequestStateAdminOAuthProvider.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteOAuthProvider"></a>
# **deleteOAuthProvider**
> RequestStateSuccessResponse deleteOAuthProvider(deleteOAuthProvider)

Delete an oauth provider method

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val deleteOAuthProvider : DeleteOAuthProvider =  // DeleteOAuthProvider | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteOAuthProvider(deleteOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#deleteOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#deleteOAuthProvider")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteOAuthProvider** | [**DeleteOAuthProvider**](DeleteOAuthProvider.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editOAuthProvider"></a>
# **editOAuthProvider**
> RequestStateAdminOAuthProvider editOAuthProvider(editOAuthProvider)

Edit an existing oauth provider method

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val editOAuthProvider : EditOAuthProvider =  // EditOAuthProvider | 
try {
    val result : RequestStateAdminOAuthProvider = apiInstance.editOAuthProvider(editOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#editOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#editOAuthProvider")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editOAuthProvider** | [**EditOAuthProvider**](EditOAuthProvider.md)|  | |

### Return type

[**RequestStateAdminOAuthProvider**](RequestStateAdminOAuthProvider.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getFederatedInstances"></a>
# **getFederatedInstances**
> RequestStatePagedResponseFederatedInstanceView getFederatedInstances(kind, limit, pageCursor, domainFilter)

Fetch federated instances.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val kind : GetFederatedInstancesKind =  // GetFederatedInstancesKind | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val domainFilter : kotlin.String = domainFilter_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseFederatedInstanceView = apiInstance.getFederatedInstances(kind, limit, pageCursor, domainFilter)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#getFederatedInstances")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#getFederatedInstances")
    e.printStackTrace()
}
```

### Parameters
| **kind** | [**GetFederatedInstancesKind**](.md)|  | [enum: all, linked, allowed, blocked] |
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **domainFilter** | **kotlin.String**|  | [optional] |

### Return type

[**RequestStatePagedResponseFederatedInstanceView**](RequestStatePagedResponseFederatedInstanceView.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getModlog"></a>
# **getModlog**
> RequestStatePagedResponseModlogView getModlog(limit, pageCursor, bulkActionParentId, showBulk, commentId, postId, otherPersonId, listingType, type, communityId, modPersonId)

Get the modlog.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val bulkActionParentId : kotlin.Double = 1.2 // kotlin.Double | Return only child entries triggered by this parent modlog action.
val showBulk : kotlin.Boolean = true // kotlin.Boolean | When `true` show all. When `false` or `None`, hide bulk actions (default).
val commentId : kotlin.Double = 1.2 // kotlin.Double | Filter by comment.
val postId : kotlin.Double = 1.2 // kotlin.Double | Filter by post. Will include comments of that post.
val otherPersonId : kotlin.Double = 1.2 // kotlin.Double | Filter by the other / modded person.
val listingType : ListingType =  // ListingType | Filter by listing type. When not using All, it will remove the non-community modlog entries, such as site bans, instance blocks, adding an admin, etc.
val type : ModlogKindFilter =  // ModlogKindFilter | Filter by the modlog action type.
val communityId : kotlin.Double = 1.2 // kotlin.Double | Filter by the community.
val modPersonId : kotlin.Double = 1.2 // kotlin.Double | Filter by the moderator.
try {
    val result : RequestStatePagedResponseModlogView = apiInstance.getModlog(limit, pageCursor, bulkActionParentId, showBulk, commentId, postId, otherPersonId, listingType, type, communityId, modPersonId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#getModlog")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#getModlog")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **bulkActionParentId** | **kotlin.Double**| Return only child entries triggered by this parent modlog action. | [optional] |
| **showBulk** | **kotlin.Boolean**| When &#x60;true&#x60; show all. When &#x60;false&#x60; or &#x60;None&#x60;, hide bulk actions (default). | [optional] |
| **commentId** | **kotlin.Double**| Filter by comment. | [optional] |
| **postId** | **kotlin.Double**| Filter by post. Will include comments of that post. | [optional] |
| **otherPersonId** | **kotlin.Double**| Filter by the other / modded person. | [optional] |
| **listingType** | [**ListingType**](.md)| Filter by listing type. When not using All, it will remove the non-community modlog entries, such as site bans, instance blocks, adding an admin, etc. | [optional] [enum: all, local, subscribed, moderator_view, suggested] |
| **type** | [**ModlogKindFilter**](.md)| Filter by the modlog action type. | [optional] |
| **communityId** | **kotlin.Double**| Filter by the community. | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **modPersonId** | **kotlin.Double**| Filter by the moderator. | [optional] |

### Return type

[**RequestStatePagedResponseModlogView**](RequestStatePagedResponseModlogView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getSiteMetadata"></a>
# **getSiteMetadata**
> RequestStateGetSiteMetadataResponse getSiteMetadata(url)

Fetch metadata for any given site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val url : kotlin.String = url_example // kotlin.String | 
try {
    val result : RequestStateGetSiteMetadataResponse = apiInstance.getSiteMetadata(url)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#getSiteMetadata")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#getSiteMetadata")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **url** | **kotlin.String**|  | |

### Return type

[**RequestStateGetSiteMetadataResponse**](RequestStateGetSiteMetadataResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listUsers"></a>
# **listUsers**
> RequestStatePagedResponseLocalUserView listUsers(limit, sort, pageCursor, bannedOnly)

Get a list of users.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val sort : LocalUserSortType =  // LocalUserSortType | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val bannedOnly : kotlin.Boolean = true // kotlin.Boolean | 
try {
    val result : RequestStatePagedResponseLocalUserView = apiInstance.listUsers(limit, sort, pageCursor, bannedOnly)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#listUsers")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#listUsers")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **sort** | [**LocalUserSortType**](.md)|  | [optional] [enum: new, old] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **bannedOnly** | **kotlin.Boolean**|  | [optional] |

### Return type

[**RequestStatePagedResponseLocalUserView**](RequestStatePagedResponseLocalUserView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="resolveObject"></a>
# **resolveObject**
> RequestStateResolveObjectView resolveObject(q)

Fetch a non-local / federated object.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val q : kotlin.String = q_example // kotlin.String | Can be the full url, or a shortened version like: !fediverse@lemmy.ml
try {
    val result : RequestStateResolveObjectView = apiInstance.resolveObject(q)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#resolveObject")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#resolveObject")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **q** | **kotlin.String**| Can be the full url, or a shortened version like: !fediverse@lemmy.ml | |

### Return type

[**RequestStateResolveObjectView**](RequestStateResolveObjectView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="search"></a>
# **search**
> RequestStateSearchResponse search(searchTerm, limit, pageCursor, showNsfw, postUrlOnly, titleOnly, listingType, timeRangeSeconds, type, creatorUsername, creatorId, communityName, communityId)

Search lemmy. If &#x60;search_term&#x60; is a url it also attempts to fetch it, just like &#x60;resolve_object&#x60;.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MiscellaneousApi()
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | The search query. Can be a plain text, or an object ID which will be resolved (eg `https://lemmy.world/comment/1` or `!fediverse@lemmy.ml`).
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val showNsfw : kotlin.Boolean = true // kotlin.Boolean | If true, then show the nsfw posts (even if your user setting is to hide them)
val postUrlOnly : kotlin.Boolean = true // kotlin.Boolean | 
val titleOnly : kotlin.Boolean = true // kotlin.Boolean | 
val listingType : ListingType =  // ListingType | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute.
val type : SearchType =  // SearchType | 
val creatorUsername : kotlin.String = creatorUsername_example // kotlin.String | 
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val communityName : kotlin.String = communityName_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : RequestStateSearchResponse = apiInstance.search(searchTerm, limit, pageCursor, showNsfw, postUrlOnly, titleOnly, listingType, timeRangeSeconds, type, creatorUsername, creatorId, communityName, communityId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MiscellaneousApi#search")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MiscellaneousApi#search")
    e.printStackTrace()
}
```

### Parameters
| **searchTerm** | **kotlin.String**| The search query. Can be a plain text, or an object ID which will be resolved (eg &#x60;https://lemmy.world/comment/1&#x60; or &#x60;!fediverse@lemmy.ml&#x60;). | |
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **showNsfw** | **kotlin.Boolean**| If true, then show the nsfw posts (even if your user setting is to hide them) | [optional] |
| **postUrlOnly** | **kotlin.Boolean**|  | [optional] |
| **titleOnly** | **kotlin.Boolean**|  | [optional] |
| **listingType** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. | [optional] |
| **type** | [**SearchType**](.md)|  | [optional] [enum: all, comments, posts, communities, users, multi_communities] |
| **creatorUsername** | **kotlin.String**|  | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **communityName** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **communityId** | **kotlin.Double**|  | [optional] |

### Return type

[**RequestStateSearchResponse**](RequestStateSearchResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

