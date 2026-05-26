# AdminApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**addAdmin**](AdminApi.md#addAdmin) | **POST** /api/v4/admin/add | Add an admin to your site. |
| [**adminAllowInstance**](AdminApi.md#adminAllowInstance) | **POST** /api/v4/admin/instance/allow | Globally allow an instance as admin. |
| [**adminBlockInstance**](AdminApi.md#adminBlockInstance) | **POST** /api/v4/admin/instance/block | Globally block an instance as admin. |
| [**approveRegistrationApplication**](AdminApi.md#approveRegistrationApplication) | **PUT** /api/v4/admin/registration_application/approve | Approve a registration application |
| [**banPerson**](AdminApi.md#banPerson) | **POST** /api/v4/admin/ban | Ban a person from your site. |
| [**createTagline**](AdminApi.md#createTagline) | **POST** /api/v4/admin/tagline | Create a new tagline |
| [**deleteMediaAdmin**](AdminApi.md#deleteMediaAdmin) | **DELETE** /api/v4/image | Delete any media. (Admin only) |
| [**deleteTagline**](AdminApi.md#deleteTagline) | **DELETE** /api/v4/admin/tagline | Delete a tagline |
| [**editTagline**](AdminApi.md#editTagline) | **PUT** /api/v4/admin/tagline | Edit an existing tagline |
| [**getRegistrationApplication**](AdminApi.md#getRegistrationApplication) | **GET** /api/v4/admin/registration_application | Get the application a user submitted when they first registered their account |
| [**hideCommunity**](AdminApi.md#hideCommunity) | **PUT** /api/v4/community/hide | Hide a community from public / \&quot;All\&quot; view. Admins only. |
| [**leaveAdmin**](AdminApi.md#leaveAdmin) | **POST** /api/v4/admin/leave | Leave the Site admins. |
| [**listCommentLikes**](AdminApi.md#listCommentLikes) | **GET** /api/v4/comment/like/list | List a comment&#39;s likes. Admin-only. |
| [**listMediaAdmin**](AdminApi.md#listMediaAdmin) | **GET** /api/v4/image/list | List all the media known to your instance. |
| [**listPostLikes**](AdminApi.md#listPostLikes) | **GET** /api/v4/post/like/list | List a post&#39;s likes. Admin-only. |
| [**listRegistrationApplications**](AdminApi.md#listRegistrationApplications) | **GET** /api/v4/admin/registration_application/list | List the registration applications. |
| [**listReports**](AdminApi.md#listReports) | **GET** /api/v4/report/list | List user reports. |
| [**listTaglines**](AdminApi.md#listTaglines) | **GET** /api/v4/admin/tagline/list | List taglines. |
| [**listUsers**](AdminApi.md#listUsers) | **GET** /api/v4/admin/users | Get a list of users. |
| [**purgeComment**](AdminApi.md#purgeComment) | **POST** /api/v4/admin/purge/comment | Purge / Delete a comment from the database. |
| [**purgeCommunity**](AdminApi.md#purgeCommunity) | **POST** /api/v4/admin/purge/community | Purge / Delete a community from the database. |
| [**purgePerson**](AdminApi.md#purgePerson) | **POST** /api/v4/admin/purge/person | Purge / Delete a person from the database. |
| [**purgePost**](AdminApi.md#purgePost) | **POST** /api/v4/admin/purge/post | Purge / Delete a post from the database. |
| [**resolveCommunityReport**](AdminApi.md#resolveCommunityReport) | **PUT** /api/v4/community/report/resolve | Resolve a report for a private message. |
| [**resolvePrivateMessageReport**](AdminApi.md#resolvePrivateMessageReport) | **PUT** /api/v4/private_message/report/resolve | Resolve a report for a private message. |


<a id="addAdmin"></a>
# **addAdmin**
> RequestStateAddAdminResponse addAdmin(addAdmin)

Add an admin to your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val addAdmin : AddAdmin =  // AddAdmin | 
try {
    val result : RequestStateAddAdminResponse = apiInstance.addAdmin(addAdmin)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#addAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#addAdmin")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **addAdmin** | [**AddAdmin**](AddAdmin.md)|  | |

### Return type

[**RequestStateAddAdminResponse**](RequestStateAddAdminResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="adminAllowInstance"></a>
# **adminAllowInstance**
> RequestStateSuccessResponse adminAllowInstance(adminAllowInstanceParams)

Globally allow an instance as admin.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val adminAllowInstanceParams : AdminAllowInstanceParams =  // AdminAllowInstanceParams | 
try {
    val result : RequestStateSuccessResponse = apiInstance.adminAllowInstance(adminAllowInstanceParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#adminAllowInstance")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#adminAllowInstance")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminAllowInstanceParams** | [**AdminAllowInstanceParams**](AdminAllowInstanceParams.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="adminBlockInstance"></a>
# **adminBlockInstance**
> RequestStateSuccessResponse adminBlockInstance(adminBlockInstanceParams)

Globally block an instance as admin.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val adminBlockInstanceParams : AdminBlockInstanceParams =  // AdminBlockInstanceParams | 
try {
    val result : RequestStateSuccessResponse = apiInstance.adminBlockInstance(adminBlockInstanceParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#adminBlockInstance")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#adminBlockInstance")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **adminBlockInstanceParams** | [**AdminBlockInstanceParams**](AdminBlockInstanceParams.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="approveRegistrationApplication"></a>
# **approveRegistrationApplication**
> RequestStateRegistrationApplicationResponse approveRegistrationApplication(approveRegistrationApplication)

Approve a registration application

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val approveRegistrationApplication : ApproveRegistrationApplication =  // ApproveRegistrationApplication | 
try {
    val result : RequestStateRegistrationApplicationResponse = apiInstance.approveRegistrationApplication(approveRegistrationApplication)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#approveRegistrationApplication")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#approveRegistrationApplication")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **approveRegistrationApplication** | [**ApproveRegistrationApplication**](ApproveRegistrationApplication.md)|  | |

### Return type

[**RequestStateRegistrationApplicationResponse**](RequestStateRegistrationApplicationResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="banPerson"></a>
# **banPerson**
> RequestStatePersonResponse banPerson(banPerson)

Ban a person from your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val banPerson : BanPerson =  // BanPerson | 
try {
    val result : RequestStatePersonResponse = apiInstance.banPerson(banPerson)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#banPerson")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#banPerson")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **banPerson** | [**BanPerson**](BanPerson.md)|  | |

### Return type

[**RequestStatePersonResponse**](RequestStatePersonResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createTagline"></a>
# **createTagline**
> RequestStateTaglineResponse createTagline(createTagline)

Create a new tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val createTagline : CreateTagline =  // CreateTagline | 
try {
    val result : RequestStateTaglineResponse = apiInstance.createTagline(createTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#createTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#createTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createTagline** | [**CreateTagline**](CreateTagline.md)|  | |

### Return type

[**RequestStateTaglineResponse**](RequestStateTaglineResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteMediaAdmin"></a>
# **deleteMediaAdmin**
> RequestStateSuccessResponse deleteMediaAdmin(deleteImageParamsI)

Delete any media. (Admin only)

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteMediaAdmin(deleteImageParamsI)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#deleteMediaAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#deleteMediaAdmin")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteImageParamsI** | [**DeleteImageParamsI**](DeleteImageParamsI.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteTagline"></a>
# **deleteTagline**
> RequestStateSuccessResponse deleteTagline(deleteTagline)

Delete a tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val deleteTagline : DeleteTagline =  // DeleteTagline | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteTagline(deleteTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#deleteTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#deleteTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteTagline** | [**DeleteTagline**](DeleteTagline.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editTagline"></a>
# **editTagline**
> RequestStateTaglineResponse editTagline(editTagline)

Edit an existing tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val editTagline : EditTagline =  // EditTagline | 
try {
    val result : RequestStateTaglineResponse = apiInstance.editTagline(editTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#editTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#editTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editTagline** | [**EditTagline**](EditTagline.md)|  | |

### Return type

[**RequestStateTaglineResponse**](RequestStateTaglineResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getRegistrationApplication"></a>
# **getRegistrationApplication**
> RequestStateRegistrationApplicationResponse getRegistrationApplication(personId)

Get the application a user submitted when they first registered their account

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val personId : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : RequestStateRegistrationApplicationResponse = apiInstance.getRegistrationApplication(personId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#getRegistrationApplication")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#getRegistrationApplication")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **personId** | **kotlin.Double**|  | |

### Return type

[**RequestStateRegistrationApplicationResponse**](RequestStateRegistrationApplicationResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="hideCommunity"></a>
# **hideCommunity**
> RequestStateSuccessResponse hideCommunity(hideCommunity)

Hide a community from public / \&quot;All\&quot; view. Admins only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val hideCommunity : HideCommunity =  // HideCommunity | 
try {
    val result : RequestStateSuccessResponse = apiInstance.hideCommunity(hideCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#hideCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#hideCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **hideCommunity** | [**HideCommunity**](HideCommunity.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="leaveAdmin"></a>
# **leaveAdmin**
> RequestStateGetSiteResponse leaveAdmin()

Leave the Site admins.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
try {
    val result : RequestStateGetSiteResponse = apiInstance.leaveAdmin()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#leaveAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#leaveAdmin")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**RequestStateGetSiteResponse**](RequestStateGetSiteResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listCommentLikes"></a>
# **listCommentLikes**
> RequestStatePagedResponseVoteView listCommentLikes(commentId, limit, pageCursor)

List a comment&#39;s likes. Admin-only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val commentId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseVoteView = apiInstance.listCommentLikes(commentId, limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listCommentLikes")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listCommentLikes")
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

[**RequestStatePagedResponseVoteView**](RequestStatePagedResponseVoteView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listMediaAdmin"></a>
# **listMediaAdmin**
> RequestStatePagedResponseLocalImageView listMediaAdmin(limit, pageCursor)

List all the media known to your instance.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseLocalImageView = apiInstance.listMediaAdmin(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listMediaAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listMediaAdmin")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**RequestStatePagedResponseLocalImageView**](RequestStatePagedResponseLocalImageView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPostLikes"></a>
# **listPostLikes**
> RequestStatePagedResponseVoteView listPostLikes(postId, limit, pageCursor)

List a post&#39;s likes. Admin-only.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val postId : kotlin.Double = 1.2 // kotlin.Double | 
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseVoteView = apiInstance.listPostLikes(postId, limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listPostLikes")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listPostLikes")
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

[**RequestStatePagedResponseVoteView**](RequestStatePagedResponseVoteView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listRegistrationApplications"></a>
# **listRegistrationApplications**
> RequestStatePagedResponseRegistrationApplicationView listRegistrationApplications(limit, pageCursor, unreadOnly)

List the registration applications.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val unreadOnly : kotlin.Boolean = true // kotlin.Boolean | Only shows the unread applications (IE those without an admin actor)
try {
    val result : RequestStatePagedResponseRegistrationApplicationView = apiInstance.listRegistrationApplications(limit, pageCursor, unreadOnly)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listRegistrationApplications")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listRegistrationApplications")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **unreadOnly** | **kotlin.Boolean**| Only shows the unread applications (IE those without an admin actor) | [optional] |

### Return type

[**RequestStatePagedResponseRegistrationApplicationView**](RequestStatePagedResponseRegistrationApplicationView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listReports"></a>
# **listReports**
> RequestStatePagedResponseReportCombinedView listReports(myReportsOnly, showCommunityRuleViolations, limit, pageCursor, communityId, postId, type, unresolvedOnly)

List user reports.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val myReportsOnly : kotlin.Boolean = true // kotlin.Boolean | If true, view all your created reports. Works for non-admins/mods also.
val showCommunityRuleViolations : kotlin.Boolean = true // kotlin.Boolean | Only for admins: also show reports with `violates_instance_rules=false`
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | if no community is given, it returns reports for all communities moderated by the auth user
val postId : kotlin.Double = 1.2 // kotlin.Double | Filter by the post id. Can return either comment or post reports.
val type : ReportType =  // ReportType | Filter the type of report.
val unresolvedOnly : kotlin.Boolean = true // kotlin.Boolean | Only shows the unresolved reports
try {
    val result : RequestStatePagedResponseReportCombinedView = apiInstance.listReports(myReportsOnly, showCommunityRuleViolations, limit, pageCursor, communityId, postId, type, unresolvedOnly)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listReports")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listReports")
    e.printStackTrace()
}
```

### Parameters
| **myReportsOnly** | **kotlin.Boolean**| If true, view all your created reports. Works for non-admins/mods also. | [optional] |
| **showCommunityRuleViolations** | **kotlin.Boolean**| Only for admins: also show reports with &#x60;violates_instance_rules&#x3D;false&#x60; | [optional] |
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **communityId** | **kotlin.Double**| if no community is given, it returns reports for all communities moderated by the auth user | [optional] |
| **postId** | **kotlin.Double**| Filter by the post id. Can return either comment or post reports. | [optional] |
| **type** | [**ReportType**](.md)| Filter the type of report. | [optional] [enum: all, posts, comments, private_messages, communities] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **unresolvedOnly** | **kotlin.Boolean**| Only shows the unresolved reports | [optional] |

### Return type

[**RequestStatePagedResponseReportCombinedView**](RequestStatePagedResponseReportCombinedView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listTaglines"></a>
# **listTaglines**
> RequestStatePagedResponseTagline listTaglines(limit, pageCursor)

List taglines.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseTagline = apiInstance.listTaglines(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listTaglines")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listTaglines")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**RequestStatePagedResponseTagline**](RequestStatePagedResponseTagline.md)

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

val apiInstance = AdminApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val sort : LocalUserSortType =  // LocalUserSortType | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val bannedOnly : kotlin.Boolean = true // kotlin.Boolean | 
try {
    val result : RequestStatePagedResponseLocalUserView = apiInstance.listUsers(limit, sort, pageCursor, bannedOnly)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#listUsers")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#listUsers")
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

<a id="purgeComment"></a>
# **purgeComment**
> RequestStateSuccessResponse purgeComment(purgeComment)

Purge / Delete a comment from the database.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val purgeComment : PurgeComment =  // PurgeComment | 
try {
    val result : RequestStateSuccessResponse = apiInstance.purgeComment(purgeComment)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#purgeComment")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#purgeComment")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **purgeComment** | [**PurgeComment**](PurgeComment.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="purgeCommunity"></a>
# **purgeCommunity**
> RequestStateSuccessResponse purgeCommunity(purgeCommunity)

Purge / Delete a community from the database.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val purgeCommunity : PurgeCommunity =  // PurgeCommunity | 
try {
    val result : RequestStateSuccessResponse = apiInstance.purgeCommunity(purgeCommunity)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#purgeCommunity")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#purgeCommunity")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **purgeCommunity** | [**PurgeCommunity**](PurgeCommunity.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="purgePerson"></a>
# **purgePerson**
> RequestStateSuccessResponse purgePerson(purgePerson)

Purge / Delete a person from the database.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val purgePerson : PurgePerson =  // PurgePerson | 
try {
    val result : RequestStateSuccessResponse = apiInstance.purgePerson(purgePerson)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#purgePerson")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#purgePerson")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **purgePerson** | [**PurgePerson**](PurgePerson.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="purgePost"></a>
# **purgePost**
> RequestStateSuccessResponse purgePost(purgePost)

Purge / Delete a post from the database.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val purgePost : PurgePost =  // PurgePost | 
try {
    val result : RequestStateSuccessResponse = apiInstance.purgePost(purgePost)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#purgePost")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#purgePost")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **purgePost** | [**PurgePost**](PurgePost.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolveCommunityReport"></a>
# **resolveCommunityReport**
> RequestStateCommunityReportResponse resolveCommunityReport(resolveCommunityReport)

Resolve a report for a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val resolveCommunityReport : ResolveCommunityReport =  // ResolveCommunityReport | 
try {
    val result : RequestStateCommunityReportResponse = apiInstance.resolveCommunityReport(resolveCommunityReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#resolveCommunityReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#resolveCommunityReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolveCommunityReport** | [**ResolveCommunityReport**](ResolveCommunityReport.md)|  | |

### Return type

[**RequestStateCommunityReportResponse**](RequestStateCommunityReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="resolvePrivateMessageReport"></a>
# **resolvePrivateMessageReport**
> RequestStatePrivateMessageReportResponse resolvePrivateMessageReport(resolvePrivateMessageReport)

Resolve a report for a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = AdminApi()
val resolvePrivateMessageReport : ResolvePrivateMessageReport =  // ResolvePrivateMessageReport | 
try {
    val result : RequestStatePrivateMessageReportResponse = apiInstance.resolvePrivateMessageReport(resolvePrivateMessageReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling AdminApi#resolvePrivateMessageReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling AdminApi#resolvePrivateMessageReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **resolvePrivateMessageReport** | [**ResolvePrivateMessageReport**](ResolvePrivateMessageReport.md)|  | |

### Return type

[**RequestStatePrivateMessageReportResponse**](RequestStatePrivateMessageReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

