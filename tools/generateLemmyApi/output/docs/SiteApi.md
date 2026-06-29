# SiteApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createSite**](SiteApi.md#createSite) | **POST** /api/v4/site | Create your site. |
| [**deleteSiteBanner**](SiteApi.md#deleteSiteBanner) | **DELETE** /api/v4/site/banner | Delete the site banner. |
| [**deleteSiteIcon**](SiteApi.md#deleteSiteIcon) | **DELETE** /api/v4/site/icon | Delete the site icon. |
| [**editSite**](SiteApi.md#editSite) | **PUT** /api/v4/site | Edit your site. |
| [**getSite**](SiteApi.md#getSite) | **GET** /api/v4/site | Gets the site, and your user data. |
| [**nodeinfo**](SiteApi.md#nodeinfo) | **GET** /nodeinfo/2.1 | Metadata for the instance |
| [**uploadSiteBanner**](SiteApi.md#uploadSiteBanner) | **POST** /api/v4/site/banner | Upload new site banner. |
| [**uploadSiteIcon**](SiteApi.md#uploadSiteIcon) | **POST** /api/v4/site/icon | Upload new site icon. |


<a id="createSite"></a>
# **createSite**
> SiteResponse createSite(createSite)

Create your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
val createSite : CreateSite =  // CreateSite | 
try {
    val result : SiteResponse = apiInstance.createSite(createSite)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#createSite")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#createSite")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createSite** | [**CreateSite**](CreateSite.md)|  | |

### Return type

[**SiteResponse**](SiteResponse.md)

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

val apiInstance = SiteApi()
try {
    val result : SuccessResponse = apiInstance.deleteSiteBanner()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#deleteSiteBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#deleteSiteBanner")
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

val apiInstance = SiteApi()
try {
    val result : SuccessResponse = apiInstance.deleteSiteIcon()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#deleteSiteIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#deleteSiteIcon")
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

<a id="editSite"></a>
# **editSite**
> SiteResponse editSite(editSite)

Edit your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
val editSite : EditSite =  // EditSite | 
try {
    val result : SiteResponse = apiInstance.editSite(editSite)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#editSite")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#editSite")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editSite** | [**EditSite**](EditSite.md)|  | |

### Return type

[**SiteResponse**](SiteResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getSite"></a>
# **getSite**
> GetSiteResponse getSite()

Gets the site, and your user data.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
try {
    val result : GetSiteResponse = apiInstance.getSite()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#getSite")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#getSite")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**GetSiteResponse**](GetSiteResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="nodeinfo"></a>
# **nodeinfo**
> NodeInfo nodeinfo()

Metadata for the instance

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
try {
    val result : NodeInfo = apiInstance.nodeinfo()
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#nodeinfo")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#nodeinfo")
    e.printStackTrace()
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**NodeInfo**](NodeInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
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

val apiInstance = SiteApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadSiteBanner(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#uploadSiteBanner")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#uploadSiteBanner")
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

val apiInstance = SiteApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : UploadImageResponse = apiInstance.uploadSiteIcon(image)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling SiteApi#uploadSiteIcon")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling SiteApi#uploadSiteIcon")
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

