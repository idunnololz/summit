# CommentApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createComment**](CommentApi.md#createComment) | **POST** /api/v4/comment | Create a comment. |
| [**createCommentReport**](CommentApi.md#createCommentReport) | **POST** /api/v4/comment/report | Report a comment. |
| [**deleteComment**](CommentApi.md#deleteComment) | **DELETE** /api/v4/comment | Delete a comment. |
| [**distinguishComment**](CommentApi.md#distinguishComment) | **POST** /api/v4/comment/distinguish | Distinguishes a comment (speak as moderator) |
| [**editComment**](CommentApi.md#editComment) | **PUT** /api/v4/comment | Edit a comment. |
| [**getComment**](CommentApi.md#getComment) | **GET** /api/v4/comment | Get / fetch comment. |
| [**getComments**](CommentApi.md#getComments) | **GET** /api/v4/comment/list | Get / fetch comments. |
| [**getCommentsSlim**](CommentApi.md#getCommentsSlim) | **GET** /api/v4/comment/list/slim | Get / fetch comments, but without the post or community. |
| [**likeComment**](CommentApi.md#likeComment) | **POST** /api/v4/comment/like |  |
| [**listCommentLikes**](CommentApi.md#listCommentLikes) | **GET** /api/v4/comment/like/list | List a comment&#39;s likes. Admin-only. |
| [**lockComment**](CommentApi.md#lockComment) | **POST** /api/v4/comment/lock | A moderator can lock a comment (IE disable replies). |
| [**removeComment**](CommentApi.md#removeComment) | **POST** /api/v4/comment/remove | A moderator remove for a comment. |
| [**resolveCommentReport**](CommentApi.md#resolveCommentReport) | **PUT** /api/v4/comment/report/resolve | Resolve a comment report. Only a mod can do this. |
| [**saveComment**](CommentApi.md#saveComment) | **PUT** /api/v4/comment/save | Save a comment. |
| [**warnComment**](CommentApi.md#warnComment) | **POST** /api/v4/comment/warn | Creates a warning against a comment and notifies the user. |


<a id="createComment"></a>
# **createComment**
> CommentResponse createComment(createComment)

Create a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val createComment : CreateComment =  // CreateComment | 
try {
    val result : CommentResponse = apiInstance.createComment(createComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#createComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#createComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createComment** | [**CreateComment**](CreateComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createCommentReport"></a>
# **createCommentReport**
> CommentReportResponse createCommentReport(createCommentReport)

Report a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val createCommentReport : CreateCommentReport =  // CreateCommentReport | 
try {
    val result : CommentReportResponse = apiInstance.createCommentReport(createCommentReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#createCommentReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#createCommentReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommentReport** | [**CreateCommentReport**](CreateCommentReport.md)|  | |

### Return type

[**CommentReportResponse**](CommentReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteComment"></a>
# **deleteComment**
> CommentResponse deleteComment(deleteComment)

Delete a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val deleteComment : DeleteComment =  // DeleteComment | 
try {
    val result : CommentResponse = apiInstance.deleteComment(deleteComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#deleteComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#deleteComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteComment** | [**DeleteComment**](DeleteComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="distinguishComment"></a>
# **distinguishComment**
> CommentResponse distinguishComment(distinguishComment)

Distinguishes a comment (speak as moderator)

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val distinguishComment : DistinguishComment =  // DistinguishComment | 
try {
    val result : CommentResponse = apiInstance.distinguishComment(distinguishComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#distinguishComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#distinguishComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **distinguishComment** | [**DistinguishComment**](DistinguishComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editComment"></a>
# **editComment**
> CommentResponse editComment(editComment)

Edit a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val editComment : EditComment =  // EditComment | 
try {
    val result : CommentResponse = apiInstance.editComment(editComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#editComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#editComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editComment** | [**EditComment**](EditComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getComment"></a>
# **getComment**
> CommentResponse getComment(id)

Get / fetch comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val id : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : CommentResponse = apiInstance.getComment(id)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#getComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#getComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **id** | **kotlin.Double**|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getComments"></a>
# **getComments**
> PagedResponseCommentView getComments(searchTerm, parentId, postId, creatorUsername, creatorId, communityName, communityId, limit, pageCursor, maxDepth, timeRangeSeconds, sort, type)

Get / fetch comments.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val parentId : kotlin.Double = 1.2 // kotlin.Double | 
val postId : kotlin.Double = 1.2 // kotlin.Double | 
val creatorUsername : kotlin.String = creatorUsername_example // kotlin.String | 
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val communityName : kotlin.String = communityName_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val maxDepth : kotlin.Double = 1.2 // kotlin.Double | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute.
val sort : CommentSortType =  // CommentSortType | 
val type : ListingType =  // ListingType | 
try {
    val result : PagedResponseCommentView = apiInstance.getComments(searchTerm, parentId, postId, creatorUsername, creatorId, communityName, communityId, limit, pageCursor, maxDepth, timeRangeSeconds, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#getComments")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#getComments")
    e.printStackTrace()
}
```

### Parameters
| **searchTerm** | **kotlin.String**|  | [optional] |
| **parentId** | **kotlin.Double**|  | [optional] |
| **postId** | **kotlin.Double**|  | [optional] |
| **creatorUsername** | **kotlin.String**|  | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **communityName** | **kotlin.String**|  | [optional] |
| **communityId** | **kotlin.Double**|  | [optional] |
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **maxDepth** | **kotlin.Double**|  | [optional] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. | [optional] |
| **sort** | [**CommentSortType**](.md)|  | [optional] [enum: hot, top, new, old, controversial] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |

### Return type

[**PagedResponseCommentView**](PagedResponseCommentView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="getCommentsSlim"></a>
# **getCommentsSlim**
> PagedResponseCommentSlimView getCommentsSlim(searchTerm, parentId, postId, creatorUsername, creatorId, communityName, communityId, limit, pageCursor, maxDepth, timeRangeSeconds, sort, type)

Get / fetch comments, but without the post or community.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val parentId : kotlin.Double = 1.2 // kotlin.Double | 
val postId : kotlin.Double = 1.2 // kotlin.Double | 
val creatorUsername : kotlin.String = creatorUsername_example // kotlin.String | 
val creatorId : kotlin.Double = 1.2 // kotlin.Double | 
val communityName : kotlin.String = communityName_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val maxDepth : kotlin.Double = 1.2 // kotlin.Double | 
val timeRangeSeconds : kotlin.Double = 1.2 // kotlin.Double | Filter to within a given time range, in seconds. IE 60 would give results for the past minute.
val sort : CommentSortType =  // CommentSortType | 
val type : ListingType =  // ListingType | 
try {
    val result : PagedResponseCommentSlimView = apiInstance.getCommentsSlim(searchTerm, parentId, postId, creatorUsername, creatorId, communityName, communityId, limit, pageCursor, maxDepth, timeRangeSeconds, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#getCommentsSlim")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#getCommentsSlim")
    e.printStackTrace()
}
```

### Parameters
| **searchTerm** | **kotlin.String**|  | [optional] |
| **parentId** | **kotlin.Double**|  | [optional] |
| **postId** | **kotlin.Double**|  | [optional] |
| **creatorUsername** | **kotlin.String**|  | [optional] |
| **creatorId** | **kotlin.Double**|  | [optional] |
| **communityName** | **kotlin.String**|  | [optional] |
| **communityId** | **kotlin.Double**|  | [optional] |
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **maxDepth** | **kotlin.Double**|  | [optional] |
| **timeRangeSeconds** | **kotlin.Double**| Filter to within a given time range, in seconds. IE 60 would give results for the past minute. | [optional] |
| **sort** | [**CommentSortType**](.md)|  | [optional] [enum: hot, top, new, old, controversial] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**ListingType**](.md)|  | [optional] [enum: all, local, subscribed, moderator_view, suggested] |

### Return type

[**PagedResponseCommentSlimView**](PagedResponseCommentSlimView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="likeComment"></a>
# **likeComment**
> CommentResponse likeComment(createCommentLike)



### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val createCommentLike : CreateCommentLike =  // CreateCommentLike | 
try {
    val result : CommentResponse = apiInstance.likeComment(createCommentLike)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#likeComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#likeComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommentLike** | [**CreateCommentLike**](CreateCommentLike.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listCommentLikes"></a>
# **listCommentLikes**
> PagedResponseVoteView listCommentLikes(commentId, limit, pageCursor)

List a comment&#39;s likes. Admin-only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val commentId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseVoteView = apiInstance.listCommentLikes(commentId, limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#listCommentLikes")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#listCommentLikes")
    e.printStackTrace()
}
```

### Parameters
| **commentId** | **kotlin.Double**|  | |
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

<a id="lockComment"></a>
# **lockComment**
> CommentResponse lockComment(lockComment)

A moderator can lock a comment (IE disable replies).

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val lockComment : LockComment =  // LockComment | 
try {
    val result : CommentResponse = apiInstance.lockComment(lockComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#lockComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#lockComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **lockComment** | [**LockComment**](LockComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="removeComment"></a>
# **removeComment**
> CommentResponse removeComment(removeComment)

A moderator remove for a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val removeComment : RemoveComment =  // RemoveComment | 
try {
    val result : CommentResponse = apiInstance.removeComment(removeComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#removeComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#removeComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **removeComment** | [**RemoveComment**](RemoveComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolveCommentReport"></a>
# **resolveCommentReport**
> CommentReportResponse resolveCommentReport(resolveCommentReport)

Resolve a comment report. Only a mod can do this.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val resolveCommentReport : ResolveCommentReport =  // ResolveCommentReport | 
try {
    val result : CommentReportResponse = apiInstance.resolveCommentReport(resolveCommentReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#resolveCommentReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#resolveCommentReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolveCommentReport** | [**ResolveCommentReport**](ResolveCommentReport.md)|  | |

### Return type

[**CommentReportResponse**](CommentReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="saveComment"></a>
# **saveComment**
> CommentResponse saveComment(saveComment)

Save a comment.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val saveComment : SaveComment =  // SaveComment | 
try {
    val result : CommentResponse = apiInstance.saveComment(saveComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#saveComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#saveComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **saveComment** | [**SaveComment**](SaveComment.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="warnComment"></a>
# **warnComment**
> CommentResponse warnComment(createCommentWarning)

Creates a warning against a comment and notifies the user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CommentApi()
val createCommentWarning : CreateCommentWarning =  // CreateCommentWarning | 
try {
    val result : CommentResponse = apiInstance.warnComment(createCommentWarning)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CommentApi#warnComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CommentApi#warnComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCommentWarning** | [**CreateCommentWarning**](CreateCommentWarning.md)|  | |

### Return type

[**CommentResponse**](CommentResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

