# MediaApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**deleteCommunityBanner**](MediaApi.md#deleteCommunityBanner) | **DELETE** /api/v4/community/banner | Delete the community banner. |
| [**deleteCommunityIcon**](MediaApi.md#deleteCommunityIcon) | **DELETE** /api/v4/community/icon | Delete the community icon. |
| [**deleteMedia**](MediaApi.md#deleteMedia) | **DELETE** /api/v4/account/media | Delete media for your account. |
| [**deleteMediaAdmin**](MediaApi.md#deleteMediaAdmin) | **DELETE** /api/v4/image | Delete any media. (Admin only) |
| [**deleteSiteBanner**](MediaApi.md#deleteSiteBanner) | **DELETE** /api/v4/site/banner | Delete the site banner. |
| [**deleteSiteIcon**](MediaApi.md#deleteSiteIcon) | **DELETE** /api/v4/site/icon | Delete the site icon. |
| [**deleteUserAvatar**](MediaApi.md#deleteUserAvatar) | **DELETE** /api/v4/account/avatar | Delete the user avatar. |
| [**deleteUserBanner**](MediaApi.md#deleteUserBanner) | **DELETE** /api/v4/account/banner | Delete the user banner. |
| [**imageHealth**](MediaApi.md#imageHealth) | **GET** /api/v4/image/health | Health check for image functionality |
| [**listMedia**](MediaApi.md#listMedia) | **GET** /api/v4/account/media/list | List all the media for your account. |
| [**listMediaAdmin**](MediaApi.md#listMediaAdmin) | **GET** /api/v4/image/list | List all the media known to your instance. |
| [**uploadCommunityBanner**](MediaApi.md#uploadCommunityBanner) | **POST** /api/v4/community/banner | Upload new community banner. |
| [**uploadCommunityIcon**](MediaApi.md#uploadCommunityIcon) | **POST** /api/v4/community/icon | Upload new community icon. |
| [**uploadImage**](MediaApi.md#uploadImage) | **POST** /api/v4/image | Upload an image to the server. |
| [**uploadSiteBanner**](MediaApi.md#uploadSiteBanner) | **POST** /api/v4/site/banner | Upload new site banner. |
| [**uploadSiteIcon**](MediaApi.md#uploadSiteIcon) | **POST** /api/v4/site/icon | Upload new site icon. |
| [**uploadUserAvatar**](MediaApi.md#uploadUserAvatar) | **POST** /api/v4/account/avatar | Upload new user avatar. |
| [**uploadUserBanner**](MediaApi.md#uploadUserBanner) | **POST** /api/v4/account/banner | Upload new user banner. |


<a id="deleteCommunityBanner"></a>
# **deleteCommunityBanner**
> RequestStateSuccessResponse deleteCommunityBanner(communityIdQuery)

Delete the community banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteCommunityBanner(communityIdQuery)
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCommunityIcon"></a>
# **deleteCommunityIcon**
> RequestStateSuccessResponse deleteCommunityIcon(communityIdQuery)

Delete the community icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val communityIdQuery : CommunityIdQuery =  // CommunityIdQuery | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteCommunityIcon(communityIdQuery)
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteMedia"></a>
# **deleteMedia**
> RequestStateSuccessResponse deleteMedia(deleteImageParamsI)

Delete media for your account.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteMedia(deleteImageParamsI)
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

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

val apiInstance = MediaApi()
val deleteImageParamsI : DeleteImageParamsI =  // DeleteImageParamsI | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteMediaAdmin(deleteImageParamsI)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#deleteMediaAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#deleteMediaAdmin")
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

<a id="deleteSiteBanner"></a>
# **deleteSiteBanner**
> RequestStateSuccessResponse deleteSiteBanner()

Delete the site banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteSiteBanner()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deleteSiteIcon"></a>
# **deleteSiteIcon**
> RequestStateSuccessResponse deleteSiteIcon()

Delete the site icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteSiteIcon()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deleteUserAvatar"></a>
# **deleteUserAvatar**
> RequestStateSuccessResponse deleteUserAvatar()

Delete the user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteUserAvatar()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="deleteUserBanner"></a>
# **deleteUserBanner**
> RequestStateSuccessResponse deleteUserBanner()

Delete the user banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteUserBanner()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="imageHealth"></a>
# **imageHealth**
> RequestStateSuccessResponse imageHealth()

Health check for image functionality

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.imageHealth()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listMedia"></a>
# **listMedia**
> RequestStatePagedResponseLocalImageView listMedia(limit, pageCursor)

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
    val result : RequestStatePagedResponseLocalImageView = apiInstance.listMedia(limit, pageCursor)
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

[**RequestStatePagedResponseLocalImageView**](RequestStatePagedResponseLocalImageView.md)

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

val apiInstance = MediaApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : RequestStatePagedResponseLocalImageView = apiInstance.listMediaAdmin(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling MediaApi#listMediaAdmin")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling MediaApi#listMediaAdmin")
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

<a id="uploadCommunityBanner"></a>
# **uploadCommunityBanner**
> RequestStateUploadImageResponse uploadCommunityBanner(id, image)

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
    val result : RequestStateUploadImageResponse = apiInstance.uploadCommunityBanner(id, image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadCommunityIcon"></a>
# **uploadCommunityIcon**
> RequestStateUploadImageResponse uploadCommunityIcon(id, image)

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
    val result : RequestStateUploadImageResponse = apiInstance.uploadCommunityIcon(id, image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadImage"></a>
# **uploadImage**
> RequestStateUploadImageResponse uploadImage(image)

Upload an image to the server.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadImage(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadSiteBanner"></a>
# **uploadSiteBanner**
> RequestStateUploadImageResponse uploadSiteBanner(image)

Upload new site banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadSiteBanner(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadSiteIcon"></a>
# **uploadSiteIcon**
> RequestStateUploadImageResponse uploadSiteIcon(image)

Upload new site icon.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadSiteIcon(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadUserAvatar"></a>
# **uploadUserAvatar**
> RequestStateUploadImageResponse uploadUserAvatar(image)

Upload new user avatar.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadUserAvatar(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a id="uploadUserBanner"></a>
# **uploadUserBanner**
> RequestStateUploadImageResponse uploadUserBanner(image)

Upload new user banner.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = MediaApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadUserBanner(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

