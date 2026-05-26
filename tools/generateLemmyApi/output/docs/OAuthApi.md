# OAuthApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**authenticateWithOAuth**](OAuthApi.md#authenticateWithOAuth) | **POST** /api/v4/oauth/authenticate | Authenticate with OAuth |
| [**createOAuthProvider**](OAuthApi.md#createOAuthProvider) | **POST** /api/v4/oauth_provider | Create a new oauth provider method |
| [**deleteOAuthProvider**](OAuthApi.md#deleteOAuthProvider) | **DELETE** /api/v4/oauth_provider | Delete an oauth provider method |
| [**editOAuthProvider**](OAuthApi.md#editOAuthProvider) | **PUT** /api/v4/oauth_provider | Edit an existing oauth provider method |


<a id="authenticateWithOAuth"></a>
# **authenticateWithOAuth**
> RequestStateLoginResponse authenticateWithOAuth(authenticateWithOauth)

Authenticate with OAuth

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = OAuthApi()
val authenticateWithOauth : AuthenticateWithOauth =  // AuthenticateWithOauth | 
try {
    val result : RequestStateLoginResponse = apiInstance.authenticateWithOAuth(authenticateWithOauth)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling OAuthApi#authenticateWithOAuth")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling OAuthApi#authenticateWithOAuth")
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

val apiInstance = OAuthApi()
val createOAuthProvider : CreateOAuthProvider =  // CreateOAuthProvider | 
try {
    val result : RequestStateAdminOAuthProvider = apiInstance.createOAuthProvider(createOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling OAuthApi#createOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling OAuthApi#createOAuthProvider")
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

val apiInstance = OAuthApi()
val deleteOAuthProvider : DeleteOAuthProvider =  // DeleteOAuthProvider | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteOAuthProvider(deleteOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling OAuthApi#deleteOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling OAuthApi#deleteOAuthProvider")
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

val apiInstance = OAuthApi()
val editOAuthProvider : EditOAuthProvider =  // EditOAuthProvider | 
try {
    val result : RequestStateAdminOAuthProvider = apiInstance.editOAuthProvider(editOAuthProvider)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling OAuthApi#editOAuthProvider")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling OAuthApi#editOAuthProvider")
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

