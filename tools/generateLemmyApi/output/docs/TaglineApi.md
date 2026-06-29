# TaglineApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createTagline**](TaglineApi.md#createTagline) | **POST** /api/v4/admin/tagline | Create a new tagline |
| [**deleteTagline**](TaglineApi.md#deleteTagline) | **DELETE** /api/v4/admin/tagline | Delete a tagline |
| [**editTagline**](TaglineApi.md#editTagline) | **PUT** /api/v4/admin/tagline | Edit an existing tagline |
| [**listTaglines**](TaglineApi.md#listTaglines) | **GET** /api/v4/admin/tagline/list | List taglines. |


<a id="createTagline"></a>
# **createTagline**
> TaglineResponse createTagline(createTagline)

Create a new tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = TaglineApi()
val createTagline : CreateTagline =  // CreateTagline | 
try {
    val result : TaglineResponse = apiInstance.createTagline(createTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaglineApi#createTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaglineApi#createTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createTagline** | [**CreateTagline**](CreateTagline.md)|  | |

### Return type

[**TaglineResponse**](TaglineResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteTagline"></a>
# **deleteTagline**
> SuccessResponse deleteTagline(deleteTagline)

Delete a tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = TaglineApi()
val deleteTagline : DeleteTagline =  // DeleteTagline | 
try {
    val result : SuccessResponse = apiInstance.deleteTagline(deleteTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaglineApi#deleteTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaglineApi#deleteTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteTagline** | [**DeleteTagline**](DeleteTagline.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editTagline"></a>
# **editTagline**
> TaglineResponse editTagline(editTagline)

Edit an existing tagline

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = TaglineApi()
val editTagline : EditTagline =  // EditTagline | 
try {
    val result : TaglineResponse = apiInstance.editTagline(editTagline)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaglineApi#editTagline")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaglineApi#editTagline")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editTagline** | [**EditTagline**](EditTagline.md)|  | |

### Return type

[**TaglineResponse**](TaglineResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listTaglines"></a>
# **listTaglines**
> PagedResponseTagline listTaglines(limit, pageCursor)

List taglines.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = TaglineApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
try {
    val result : PagedResponseTagline = apiInstance.listTaglines(limit, pageCursor)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling TaglineApi#listTaglines")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling TaglineApi#listTaglines")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **pageCursor** | **kotlin.String**|  | [optional] |

### Return type

[**PagedResponseTagline**](PagedResponseTagline.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

