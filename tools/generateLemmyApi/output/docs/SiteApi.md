# SiteApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createSite**](SiteApi.md#createSite) | **POST** /api/v4/site | Create your site. |
| [**deleteSiteBanner**](SiteApi.md#deleteSiteBanner) | **DELETE** /api/v4/site/banner | Delete the site banner. |
| [**deleteSiteIcon**](SiteApi.md#deleteSiteIcon) | **DELETE** /api/v4/site/icon | Delete the site icon. |
| [**editSite**](SiteApi.md#editSite) | **PUT** /api/v4/site | Edit your site. |
| [**getSite**](SiteApi.md#getSite) | **GET** /api/v4/site | Gets the site, and your user data. |
| [**uploadSiteBanner**](SiteApi.md#uploadSiteBanner) | **POST** /api/v4/site/banner | Upload new site banner. |
| [**uploadSiteIcon**](SiteApi.md#uploadSiteIcon) | **POST** /api/v4/site/icon | Upload new site icon. |


<a id="createSite"></a>
# **createSite**
> RequestStateSiteResponse createSite(createSite)

Create your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
val createSite : CreateSite =  // CreateSite | 
try {
    val result : RequestStateSiteResponse = apiInstance.createSite(createSite)
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

[**RequestStateSiteResponse**](RequestStateSiteResponse.md)

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

val apiInstance = SiteApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteSiteBanner()
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

val apiInstance = SiteApi()
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteSiteIcon()
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

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="editSite"></a>
# **editSite**
> RequestStateSiteResponse editSite(editSite)

Edit your site.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
val editSite : EditSite =  // EditSite | 
try {
    val result : RequestStateSiteResponse = apiInstance.editSite(editSite)
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

[**RequestStateSiteResponse**](RequestStateSiteResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="getSite"></a>
# **getSite**
> RequestStateGetSiteResponse getSite()

Gets the site, and your user data.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = SiteApi()
try {
    val result : RequestStateGetSiteResponse = apiInstance.getSite()
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

[**RequestStateGetSiteResponse**](RequestStateGetSiteResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
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

val apiInstance = SiteApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadSiteBanner(image)
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

val apiInstance = SiteApi()
val image : java.io.File = BINARY_DATA_HERE // java.io.File | 
try {
    val result : RequestStateUploadImageResponse = apiInstance.uploadSiteIcon(image)
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

[**RequestStateUploadImageResponse**](RequestStateUploadImageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

