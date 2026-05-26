# PrivateMessageApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createPrivateMessage**](PrivateMessageApi.md#createPrivateMessage) | **POST** /api/v4/private_message | Create a private message. |
| [**createPrivateMessageReport**](PrivateMessageApi.md#createPrivateMessageReport) | **POST** /api/v4/private_message/report | Create a report for a private message. |
| [**deletePrivateMessage**](PrivateMessageApi.md#deletePrivateMessage) | **DELETE** /api/v4/private_message | Delete a private message. |
| [**editPrivateMessage**](PrivateMessageApi.md#editPrivateMessage) | **PUT** /api/v4/private_message | Edit a private message. |
| [**resolvePrivateMessageReport**](PrivateMessageApi.md#resolvePrivateMessageReport) | **PUT** /api/v4/private_message/report/resolve | Resolve a report for a private message. |


<a id="createPrivateMessage"></a>
# **createPrivateMessage**
> RequestStatePrivateMessageResponse createPrivateMessage(createPrivateMessage)

Create a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PrivateMessageApi()
val createPrivateMessage : CreatePrivateMessage =  // CreatePrivateMessage | 
try {
    val result : RequestStatePrivateMessageResponse = apiInstance.createPrivateMessage(createPrivateMessage)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PrivateMessageApi#createPrivateMessage")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PrivateMessageApi#createPrivateMessage")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPrivateMessage** | [**CreatePrivateMessage**](CreatePrivateMessage.md)|  | |

### Return type

[**RequestStatePrivateMessageResponse**](RequestStatePrivateMessageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="createPrivateMessageReport"></a>
# **createPrivateMessageReport**
> RequestStatePrivateMessageReportResponse createPrivateMessageReport(createPrivateMessageReport)

Create a report for a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PrivateMessageApi()
val createPrivateMessageReport : CreatePrivateMessageReport =  // CreatePrivateMessageReport | 
try {
    val result : RequestStatePrivateMessageReportResponse = apiInstance.createPrivateMessageReport(createPrivateMessageReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PrivateMessageApi#createPrivateMessageReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PrivateMessageApi#createPrivateMessageReport")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createPrivateMessageReport** | [**CreatePrivateMessageReport**](CreatePrivateMessageReport.md)|  | |

### Return type

[**RequestStatePrivateMessageReportResponse**](RequestStatePrivateMessageReportResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deletePrivateMessage"></a>
# **deletePrivateMessage**
> RequestStatePrivateMessageResponse deletePrivateMessage(deletePrivateMessage)

Delete a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PrivateMessageApi()
val deletePrivateMessage : DeletePrivateMessage =  // DeletePrivateMessage | 
try {
    val result : RequestStatePrivateMessageResponse = apiInstance.deletePrivateMessage(deletePrivateMessage)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PrivateMessageApi#deletePrivateMessage")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PrivateMessageApi#deletePrivateMessage")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deletePrivateMessage** | [**DeletePrivateMessage**](DeletePrivateMessage.md)|  | |

### Return type

[**RequestStatePrivateMessageResponse**](RequestStatePrivateMessageResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editPrivateMessage"></a>
# **editPrivateMessage**
> RequestStatePrivateMessageResponse editPrivateMessage(editPrivateMessage)

Edit a private message.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PrivateMessageApi()
val editPrivateMessage : EditPrivateMessage =  // EditPrivateMessage | 
try {
    val result : RequestStatePrivateMessageResponse = apiInstance.editPrivateMessage(editPrivateMessage)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PrivateMessageApi#editPrivateMessage")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PrivateMessageApi#editPrivateMessage")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editPrivateMessage** | [**EditPrivateMessage**](EditPrivateMessage.md)|  | |

### Return type

[**RequestStatePrivateMessageResponse**](RequestStatePrivateMessageResponse.md)

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

val apiInstance = PrivateMessageApi()
val resolvePrivateMessageReport : ResolvePrivateMessageReport =  // ResolvePrivateMessageReport | 
try {
    val result : RequestStatePrivateMessageReportResponse = apiInstance.resolvePrivateMessageReport(resolvePrivateMessageReport)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PrivateMessageApi#resolvePrivateMessageReport")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PrivateMessageApi#resolvePrivateMessageReport")
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

