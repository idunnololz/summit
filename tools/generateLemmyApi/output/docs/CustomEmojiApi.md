# CustomEmojiApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**createCustomEmoji**](CustomEmojiApi.md#createCustomEmoji) | **POST** /api/v4/custom_emoji | Create a new custom emoji. |
| [**deleteCustomEmoji**](CustomEmojiApi.md#deleteCustomEmoji) | **POST** /api/v4/custom_emoji/delete | Delete a custom emoji. |
| [**editCustomEmoji**](CustomEmojiApi.md#editCustomEmoji) | **PUT** /api/v4/custom_emoji | Edit an existing custom emoji. |
| [**listCustomEmojis**](CustomEmojiApi.md#listCustomEmojis) | **GET** /api/v4/custom_emoji/list | List custom emojis |


<a id="createCustomEmoji"></a>
# **createCustomEmoji**
> RequestStateCustomEmojiResponse createCustomEmoji(createCustomEmoji)

Create a new custom emoji.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CustomEmojiApi()
val createCustomEmoji : CreateCustomEmoji =  // CreateCustomEmoji | 
try {
    val result : RequestStateCustomEmojiResponse = apiInstance.createCustomEmoji(createCustomEmoji)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CustomEmojiApi#createCustomEmoji")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CustomEmojiApi#createCustomEmoji")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **createCustomEmoji** | [**CreateCustomEmoji**](CreateCustomEmoji.md)|  | |

### Return type

[**RequestStateCustomEmojiResponse**](RequestStateCustomEmojiResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="deleteCustomEmoji"></a>
# **deleteCustomEmoji**
> RequestStateSuccessResponse deleteCustomEmoji(deleteCustomEmoji)

Delete a custom emoji.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CustomEmojiApi()
val deleteCustomEmoji : DeleteCustomEmoji =  // DeleteCustomEmoji | 
try {
    val result : RequestStateSuccessResponse = apiInstance.deleteCustomEmoji(deleteCustomEmoji)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CustomEmojiApi#deleteCustomEmoji")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CustomEmojiApi#deleteCustomEmoji")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **deleteCustomEmoji** | [**DeleteCustomEmoji**](DeleteCustomEmoji.md)|  | |

### Return type

[**RequestStateSuccessResponse**](RequestStateSuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="editCustomEmoji"></a>
# **editCustomEmoji**
> RequestStateCustomEmojiResponse editCustomEmoji(editCustomEmoji)

Edit an existing custom emoji.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CustomEmojiApi()
val editCustomEmoji : EditCustomEmoji =  // EditCustomEmoji | 
try {
    val result : RequestStateCustomEmojiResponse = apiInstance.editCustomEmoji(editCustomEmoji)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CustomEmojiApi#editCustomEmoji")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CustomEmojiApi#editCustomEmoji")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **editCustomEmoji** | [**EditCustomEmoji**](EditCustomEmoji.md)|  | |

### Return type

[**RequestStateCustomEmojiResponse**](RequestStateCustomEmojiResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="listCustomEmojis"></a>
# **listCustomEmojis**
> RequestStateListCustomEmojisResponse listCustomEmojis(category)

List custom emojis

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = CustomEmojiApi()
val category : kotlin.String = category_example // kotlin.String | 
try {
    val result : RequestStateListCustomEmojisResponse = apiInstance.listCustomEmojis(category)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling CustomEmojiApi#listCustomEmojis")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling CustomEmojiApi#listCustomEmojis")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **category** | **kotlin.String**|  | [optional] |

### Return type

[**RequestStateListCustomEmojisResponse**](RequestStateListCustomEmojisResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

