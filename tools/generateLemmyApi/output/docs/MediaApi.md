# MediaApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**adminDeleteMedia**](MediaApi.md#adminDeleteMedia) | **DELETE** /api/v4/image | Delete any media. (Admin only) |
| [**adminListMedia**](MediaApi.md#adminListMedia) | **GET** /api/v4/image/list | List all the media known to your instance. |
| [**deleteCommunityBanner**](MediaApi.md#deleteCommunityBanner) | **DELETE** /api/v4/community/banner | Delete the community banner. |
| [**deleteCommunityIcon**](MediaApi.md#deleteCommunityIcon) | **DELETE** /api/v4/community/icon | Delete the community icon. |
| [**deleteMedia**](MediaApi.md#deleteMedia) | **DELETE** /api/v4/account/media | Delete media for your account. |
| [**deleteSiteBanner**](MediaApi.md#deleteSiteBanner) | **DELETE** /api/v4/site/banner | Delete the site banner. |
| [**deleteSiteIcon**](MediaApi.md#deleteSiteIcon) | **DELETE** /api/v4/site/icon | Delete the site icon. |
| [**deleteUserAvatar**](MediaApi.md#deleteUserAvatar) | **DELETE** /api/v4/account/avatar | Delete the user avatar. |
| [**deleteUserBanner**](MediaApi.md#deleteUserBanner) | **DELETE** /api/v4/account/banner | Delete the user banner. |
| [**imageHealth**](MediaApi.md#imageHealth) | **GET** /api/v4/image/health | Health check for image functionality |
| [**listMedia**](MediaApi.md#listMedia) | **GET** /api/v4/account/media/list | List all the media for your account. |
| [**uploadCommunityBanner**](MediaApi.md#uploadCommunityBanner) | **POST** /api/v4/community/banner | Upload new community banner. |
| [**uploadCommunityIcon**](MediaApi.md#uploadCommunityIcon) | **POST** /api/v4/community/icon | Upload new community icon. |
| [**uploadImage**](MediaApi.md#uploadImage) | **POST** /api/v4/image | Upload an image to the server. |
| [**uploadSiteBanner**](MediaApi.md#uploadSiteBanner) | **POST** /api/v4/site/banner | Upload new site banner. |
| [**uploadSiteIcon**](MediaApi.md#uploadSiteIcon) | **POST** /api/v4/site/icon | Upload new site icon. |
| [**uploadUserAvatar**](MediaApi.md#uploadUserAvatar) | **POST** /api/v4/account/avatar | Upload new user avatar. |
| [**uploadUserBanner**](MediaApi.md#uploadUserBanner) | **POST** /api/v4/account/banner | Upload new user banner. |


<a id="adminDeleteMedia"></a>
# **adminDeleteMedia**
> SuccessResponse adminDeleteMedia(deleteImageParamsI)

Delete any media. (Admin only)

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : SuccessResponse = apiInstance.adminDeleteMedia(deleteImageParamsI)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#adminDeleteMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#adminDeleteMedia")
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

<a id="adminListMedia"></a>
# **adminListMedia**
> PagedResponseLocalImageView adminListMedia(limit, pageCursor)

List all the media known to your instance.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseLocalImageView = apiInstance.adminListMedia(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#adminListMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#adminListMedia")
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

<a id="deleteCommunityBanner"></a>
# **deleteCommunityBanner**
> SuccessResponse deleteCommunityBanner(communityIdQuery)

Delete the community banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : SuccessResponse = apiInstance.deleteCommunityBanner(communityIdQuery)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteCommunityBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteCommunityBanner")
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

val apiInstance = MediaApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : SuccessResponse = apiInstance.deleteCommunityIcon(communityIdQuery)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteCommunityIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteCommunityIcon")
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

<a id="deleteMedia"></a>
# **deleteMedia**
> SuccessResponse deleteMedia(deleteImageParamsI)

Delete media for your account.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : SuccessResponse = apiInstance.deleteMedia(deleteImageParamsI)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteMedia")
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

<a id="deleteSiteBanner"></a>
# **deleteSiteBanner**
> SuccessResponse deleteSiteBanner()

Delete the site banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : SuccessResponse = apiInstance.deleteSiteBanner()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteSiteBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteSiteBanner")
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

<a id="deleteSiteIcon"></a>
# **deleteSiteIcon**
> SuccessResponse deleteSiteIcon()

Delete the site icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : SuccessResponse = apiInstance.deleteSiteIcon()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteSiteIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteSiteIcon")
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

<a id="deleteUserAvatar"></a>
# **deleteUserAvatar**
> SuccessResponse deleteUserAvatar()

Delete the user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : SuccessResponse = apiInstance.deleteUserAvatar()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteUserAvatar")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteUserAvatar")
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

val apiInstance = MediaApi()
try {
    val result : SuccessResponse = apiInstance.deleteUserBanner()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteUserBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteUserBanner")
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

<a id="imageHealth"></a>
# **imageHealth**
> SuccessResponse imageHealth()

Health check for image functionality

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : SuccessResponse = apiInstance.imageHealth()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#imageHealth")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#imageHealth")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization

No authorization required

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

val apiInstance = MediaApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseLocalImageView = apiInstance.listMedia(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#listMedia")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#listMedia")
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

<a id="uploadCommunityBanner"></a>
# **uploadCommunityBanner**
> UploadImageResponse uploadCommunityBanner(id, image)

Upload new community banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val id : kotlin.Double = 1.2 // kotlin.Double | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadCommunityBanner(id, image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadCommunityBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadCommunityBanner")
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

val apiInstance = MediaApi()
val id : kotlin.Double = 1.2 // kotlin.Double | 
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadCommunityIcon(id, image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadCommunityIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadCommunityIcon")
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

<a id="uploadImage"></a>
# **uploadImage**
> UploadImageResponse uploadImage(image)

Upload an image to the server.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadImage(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadImage")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadImage")
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

<a id="uploadSiteBanner"></a>
# **uploadSiteBanner**
> UploadImageResponse uploadSiteBanner(image)

Upload new site banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadSiteBanner(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadSiteBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadSiteBanner")
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

<a id="uploadSiteIcon"></a>
# **uploadSiteIcon**
> UploadImageResponse uploadSiteIcon(image)

Upload new site icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadSiteIcon(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadSiteIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadSiteIcon")
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

<a id="uploadUserAvatar"></a>
# **uploadUserAvatar**
> UploadImageResponse uploadUserAvatar(image)

Upload new user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadUserAvatar(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadUserAvatar")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadUserAvatar")
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

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadUserBanner(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#uploadUserBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#uploadUserBanner")
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

