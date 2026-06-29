# PostApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createPost**](PostApi.md#createPost) | **POST** /api/v4/post | Create a post. |
| [**createPostReport**](PostApi.md#createPostReport) | **POST** /api/v4/post/report | Report a post. |
| [**deletePost**](PostApi.md#deletePost) | **DELETE** /api/v4/post | Delete a post. |
| [**editPost**](PostApi.md#editPost) | **PUT** /api/v4/post | Edit a post. |
| [**editPostNotifications**](PostApi.md#editPostNotifications) | **PUT** /api/v4/post/notifications | Change notification settings for a post |
| [**featurePost**](PostApi.md#featurePost) | **POST** /api/v4/post/feature | A moderator can feature a community post ( IE stick it to the top of a community ). |
| [**getPost**](PostApi.md#getPost) | **GET** /api/v4/post | Get / fetch a post. |
| [**getPosts**](PostApi.md#getPosts) | **GET** /api/v4/post/list | Get / fetch posts, with various filters. |
| [**getSiteMetadata**](PostApi.md#getSiteMetadata) | **GET** /api/v4/post/site_metadata | Fetch metadata for any given site. |
| [**hidePost**](PostApi.md#hidePost) | **POST** /api/v4/post/hide | Hide a post from list views. |
| [**likePost**](PostApi.md#likePost) | **POST** /api/v4/post/like | Like / vote on a post. |
| [**listPostLikes**](PostApi.md#listPostLikes) | **GET** /api/v4/post/like/list | List a post&#39;s likes. Admin-only. |
| [**lockPost**](PostApi.md#lockPost) | **POST** /api/v4/post/lock | A moderator can lock a post ( IE disable new comments ). |
| [**markManyPostsAsRead**](PostApi.md#markManyPostsAsRead) | **POST** /api/v4/post/mark_as_read/many | Mark multiple posts as read. |
| [**markPostAsRead**](PostApi.md#markPostAsRead) | **POST** /api/v4/post/mark_as_read | Mark a post as read. |
| [**modEditPost**](PostApi.md#modEditPost) | **PUT** /api/v4/post/mod_edit | Mods can change nsfw flag and tags for a post |
| [**removePost**](PostApi.md#removePost) | **POST** /api/v4/post/remove | A moderator remove for a post. |
| [**resolvePostReport**](PostApi.md#resolvePostReport) | **PUT** /api/v4/post/report/resolve | Resolve a post report. Only a mod can do this. |
| [**savePost**](PostApi.md#savePost) | **PUT** /api/v4/post/save | Save a post. |
| [**warnPost**](PostApi.md#warnPost) | **POST** /api/v4/post/warn | Creates a warning against a post and notifies the user. |


<a id="createPost"></a>
# **createPost**
> PostResponse createPost(createPost)

Create a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val createPost : CreatePost =  // CreatePost | 
try {
    val result : PostResponse = apiInstance.createPost(createPost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#createPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#createPost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPost** | [**CreatePost**](CreatePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createPostReport"></a>
# **createPostReport**
> PostReportResponse createPostReport(createPostReport)

Report a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val createPostReport : CreatePostReport =  // CreatePostReport | 
try {
    val result : PostReportResponse = apiInstance.createPostReport(createPostReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#createPostReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#createPostReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPostReport** | [**CreatePostReport**](CreatePostReport.md)|  | |

### Return type

[**PostReportResponse**](PostReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deletePost"></a>
# **deletePost**
> PostResponse deletePost(deletePost)

Delete a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val deletePost : DeletePost =  // DeletePost | 
try {
    val result : PostResponse = apiInstance.deletePost(deletePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#deletePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#deletePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deletePost** | [**DeletePost**](DeletePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editPost"></a>
# **editPost**
> PostResponse editPost(editPost)

Edit a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val editPost : EditPost =  // EditPost | 
try {
    val result : PostResponse = apiInstance.editPost(editPost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#editPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#editPost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editPost** | [**EditPost**](EditPost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editPostNotifications"></a>
# **editPostNotifications**
> SuccessResponse editPostNotifications(editPostNotifications)

Change notification settings for a post

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val editPostNotifications : EditPostNotifications =  // EditPostNotifications | 
try {
    val result : SuccessResponse = apiInstance.editPostNotifications(editPostNotifications)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#editPostNotifications")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#editPostNotifications")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editPostNotifications** | [**EditPostNotifications**](EditPostNotifications.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="featurePost"></a>
# **featurePost**
> PostResponse featurePost(featurePost)

A moderator can feature a community post ( IE stick it to the top of a community ).

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val featurePost : FeaturePost =  // FeaturePost | 
try {
    val result : PostResponse = apiInstance.featurePost(featurePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#featurePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#featurePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **featurePost** | [**FeaturePost**](FeaturePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getPost"></a>
# **getPost**
> GetPostResponse getPost(commentId, id)

Get / fetch a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val commentId : kotlin.Double = 1.2 // kotlin.Double | 
val id : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : GetPostResponse = apiInstance.getPost(commentId, id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#getPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#getPost")
    e.printStackTrace()
}
```

### Parameters
| **commentId** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Double**|  | [optional] |

### Return type

[**GetPostResponse**](GetPostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getPosts"></a>
# **getPosts**
> PagedResponsePostView getPosts(limit, pageCursor, tagId, searchUrlOnly, searchTitleOnly, searchTerm, noCommentsOnly, markAsRead, hidePostsWithMedia, showNsfw, showRead, showHidden, multiCommunityName, multiCommunityId, creatorUsername, creatorId, communityName, communityId, timeRangeSeconds, sort, type)

Get / fetch posts, with various filters.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val tagId : kotlin.Double = 1.2 // kotlin.Double | Only show posts which have this tag
val searchUrlOnly : kotlin.Boolean = true // kotlin.Boolean | 
val searchTitleOnly : kotlin.Boolean = true // kotlin.Boolean | 
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val noCommentsOnly : kotlin.Boolean = true // kotlin.Boolean | If true, then only show posts with no comments
val markAsRead : kotlin.Boolean = true // kotlin.Boolean | Whether to automatically mark fetched posts as read.
val hidePostsWithMedia : kotlin.Boolean = true // kotlin.Boolean | If false, then show posts with media attached (even if your user setting is to hide them)
val showNsfw : kotlin.Boolean = true // kotlin.Boolean | If true, then show the nsfw posts (even if your user setting is to hide them)
val showRead : kotlin.Boolean = true // kotlin.Boolean | If true, then show the read posts (even if your user setting is to hide them)
val showHidden : kotlin.Boolean = true // kotlin.Boolean | 
val multiCommunityName : kotlin.String = multiCommunityName_example // kotlin.String | 
val multiCommunityId : kotlin.Double = 1.2 // kotlin.Double | 
val creatorUsername : kotlin.String = creatorUsername_example // kotlin.String | 
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val communityName : kotlin.String = communityName_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute. Use Zero to override the local_site and local_user time_range.
val sort : PostSortType =  // PostSortType | 
val type : ListingType =  // ListingType | 
try {
    val result : PagedResponsePostView = apiInstance.getPosts(limit, pageCursor, tagId, searchUrlOnly, searchTitleOnly, searchTerm, noCommentsOnly, markAsRead, hidePostsWithMedia, showNsfw, showRead, showHidden, multiCommunityName, multiCommunityId, creatorUsername, creatorId, communityName, communityId, timeRangeSeconds, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#getPosts")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#getPosts")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **tagId** | **kotlin.Double**| Only show posts which have this tag | [optional] |
| **searchUrlOnly** | **kotlin.Boolean**|  | [optional] |
| **searchTitleOnly** | **kotlin.Boolean**|  | [optional] |
| **searchTerm** | **kotlin.String**|  | [optional] |
| **noCommentsOnly** | **kotlin.Boolean**| If true, then only show posts with no comments | [optional] |
| **markAsRead** | **kotlin.Boolean**| Whether to automatically mark fetched posts as read. | [optional] |
| **hidePostsWithMedia** | **kotlin.Boolean**| If false, then show posts with media attached (even if your user setting is to hide them) | [optional] |
| **showNsfw** | **kotlin.Boolean**| If true, then show the nsfw posts (even if your user setting is to hide them) | [optional] |
| **showRead** | **kotlin.Boolean**| If true, then show the read posts (even if your user setting is to hide them) | [optional] |
| **showHidden** | **kotlin.Boolean**|  | [optional] |
| **multiCommunityName** | **kotlin.String**|  | [optional] |
| **multiCommunityId** | **kotlin.Double**|  | [optional] |
| **creatorUsername** | **kotlin.String**|  | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **communityName** | **kotlin.String**|  | [optional] |
| **communityId** | **kotlin.Double**|  | [optional] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. Use Zero to override the local_site and local_user time_range. | [optional] |
| **sort** | [**PostSortType**](.md)|  | [optional] [enum: active, hot, new, old, top, most_comments, new_comments, controversial, scaled] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |

### Return type

[**PagedResponsePostView**](PagedResponsePostView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getSiteMetadata"></a>
# **getSiteMetadata**
> GetSiteMetadataResponse getSiteMetadata(url)

Fetch metadata for any given site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val url : kotlin.String = url_example // kotlin.String | 
try {
    val result : GetSiteMetadataResponse = apiInstance.getSiteMetadata(url)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#getSiteMetadata")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#getSiteMetadata")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **url** | **kotlin.String**|  | |

### Return type

[**GetSiteMetadataResponse**](GetSiteMetadataResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="hidePost"></a>
# **hidePost**
> PostResponse hidePost(hidePost)

Hide a post from list views.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val hidePost : HidePost =  // HidePost | 
try {
    val result : PostResponse = apiInstance.hidePost(hidePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#hidePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#hidePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **hidePost** | [**HidePost**](HidePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="likePost"></a>
# **likePost**
> PostResponse likePost(createPostLike)

Like / vote on a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val createPostLike : CreatePostLike =  // CreatePostLike | 
try {
    val result : PostResponse = apiInstance.likePost(createPostLike)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#likePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#likePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPostLike** | [**CreatePostLike**](CreatePostLike.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listPostLikes"></a>
# **listPostLikes**
> PagedResponseVoteView listPostLikes(postId, limit, pageCursor)

List a post&#39;s likes. Admin-only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val postId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseVoteView = apiInstance.listPostLikes(postId, limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#listPostLikes")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#listPostLikes")
    e.printStackTrace()
}
```

### Parameters
| **postId** | **kotlin.Double**|  | |
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponseVoteView**](PagedResponseVoteView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="lockPost"></a>
# **lockPost**
> PostResponse lockPost(lockPost)

A moderator can lock a post ( IE disable new comments ).

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val lockPost : LockPost =  // LockPost | 
try {
    val result : PostResponse = apiInstance.lockPost(lockPost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#lockPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#lockPost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **lockPost** | [**LockPost**](LockPost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="markManyPostsAsRead"></a>
# **markManyPostsAsRead**
> SuccessResponse markManyPostsAsRead(markManyPostsAsRead)

Mark multiple posts as read.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val markManyPostsAsRead : MarkManyPostsAsRead =  // MarkManyPostsAsRead | 
try {
    val result : SuccessResponse = apiInstance.markManyPostsAsRead(markManyPostsAsRead)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#markManyPostsAsRead")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#markManyPostsAsRead")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **markManyPostsAsRead** | [**MarkManyPostsAsRead**](MarkManyPostsAsRead.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="markPostAsRead"></a>
# **markPostAsRead**
> PostResponse markPostAsRead(markPostAsRead)

Mark a post as read.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val markPostAsRead : MarkPostAsRead =  // MarkPostAsRead | 
try {
    val result : PostResponse = apiInstance.markPostAsRead(markPostAsRead)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#markPostAsRead")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#markPostAsRead")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **markPostAsRead** | [**MarkPostAsRead**](MarkPostAsRead.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="modEditPost"></a>
# **modEditPost**
> PostResponse modEditPost(modEditPost)

Mods can change nsfw flag and tags for a post

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val modEditPost : ModEditPost =  // ModEditPost | 
try {
    val result : PostResponse = apiInstance.modEditPost(modEditPost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#modEditPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#modEditPost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **modEditPost** | [**ModEditPost**](ModEditPost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="removePost"></a>
# **removePost**
> PostResponse removePost(removePost)

A moderator remove for a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val removePost : RemovePost =  // RemovePost | 
try {
    val result : PostResponse = apiInstance.removePost(removePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#removePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#removePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removePost** | [**RemovePost**](RemovePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolvePostReport"></a>
# **resolvePostReport**
> PostReportResponse resolvePostReport(resolvePostReport)

Resolve a post report. Only a mod can do this.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val resolvePostReport : ResolvePostReport =  // ResolvePostReport | 
try {
    val result : PostReportResponse = apiInstance.resolvePostReport(resolvePostReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#resolvePostReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#resolvePostReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolvePostReport** | [**ResolvePostReport**](ResolvePostReport.md)|  | |

### Return type

[**PostReportResponse**](PostReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="savePost"></a>
# **savePost**
> PostResponse savePost(savePost)

Save a post.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val savePost : SavePost =  // SavePost | 
try {
    val result : PostResponse = apiInstance.savePost(savePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#savePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#savePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **savePost** | [**SavePost**](SavePost.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="warnPost"></a>
# **warnPost**
> PostResponse warnPost(createPostWarning)

Creates a warning against a post and notifies the user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PostApi()
val createPostWarning : CreatePostWarning =  // CreatePostWarning | 
try {
    val result : PostResponse = apiInstance.warnPost(createPostWarning)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PostApi#warnPost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PostApi#warnPost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPostWarning** | [**CreatePostWarning**](CreatePostWarning.md)|  | |

### Return type

[**PostResponse**](PostResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

