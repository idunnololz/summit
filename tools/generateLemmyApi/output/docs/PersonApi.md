# PersonApi

All URIs are relative to *https://voyager.lemmy.ml*

| Method | HTTP request | Description |
| ------------- | ------------- | ------------- |
| [**getPersonDetails**](PersonApi.md#getPersonDetails) | **GET** /api/v4/person | Get the details for a person. |
| [**listPersonContent**](PersonApi.md#listPersonContent) | **GET** /api/v4/person/content | List the content for a person. |
| [**listPersons**](PersonApi.md#listPersons) | **GET** /api/v4/person/list | List persons. |
| [**notePerson**](PersonApi.md#notePerson) | **POST** /api/v4/person/note | Make a note for a person. |
| [**userBlockInstancePersons**](PersonApi.md#userBlockInstancePersons) | **POST** /api/v4/account/block/instance/persons | Block an instance&#39;s persons as a user. |


<a id="getPersonDetails"></a>
# **getPersonDetails**
> GetPersonDetailsResponse getPersonDetails(username, personId)

Get the details for a person.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PersonApi()
val username : kotlin.String = username_example // kotlin.String | Example: dessalines , or dessalines@xyz.tld
val personId : kotlin.Double = 1.2 // kotlin.Double | 
try {
    val result : GetPersonDetailsResponse = apiInstance.getPersonDetails(username, personId)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PersonApi#getPersonDetails")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PersonApi#getPersonDetails")
    e.printStackTrace()
}
```

### Parameters
| **username** | **kotlin.String**| Example: dessalines , or dessalines@xyz.tld | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **personId** | **kotlin.Double**|  | [optional] |

### Return type

[**GetPersonDetailsResponse**](GetPersonDetailsResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersonContent"></a>
# **listPersonContent**
> PagedResponsePostCommentCombinedView listPersonContent(limit, pageCursor, communityName, communityId, username, personId, type)

List the content for a person.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PersonApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val communityName : kotlin.String = communityName_example // kotlin.String | 
val communityId : kotlin.Double = 1.2 // kotlin.Double | 
val username : kotlin.String = username_example // kotlin.String | Example: dessalines , or dessalines@xyz.tld
val personId : kotlin.Double = 1.2 // kotlin.Double | 
val type : PersonContentType =  // PersonContentType | 
try {
    val result : PagedResponsePostCommentCombinedView = apiInstance.listPersonContent(limit, pageCursor, communityName, communityId, username, personId, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PersonApi#listPersonContent")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PersonApi#listPersonContent")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **communityName** | **kotlin.String**|  | [optional] |
| **communityId** | **kotlin.Double**|  | [optional] |
| **username** | **kotlin.String**| Example: dessalines , or dessalines@xyz.tld | [optional] |
| **personId** | **kotlin.Double**|  | [optional] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**PersonContentType**](.md)|  | [optional] [enum: all, comments, posts] |

### Return type

[**PagedResponsePostCommentCombinedView**](PagedResponsePostCommentCombinedView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="listPersons"></a>
# **listPersons**
> PagedResponsePersonView listPersons(limit, pageCursor, searchTitleOnly, searchTerm, sort, type)

List persons.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PersonApi()
val limit : kotlin.Double = 1.2 // kotlin.Double | 
val pageCursor : kotlin.String = pageCursor_example // kotlin.String | 
val searchTitleOnly : kotlin.Boolean = true // kotlin.Boolean | 
val searchTerm : kotlin.String = searchTerm_example // kotlin.String | 
val sort : PersonSortType =  // PersonSortType | 
val type : PersonListingType =  // PersonListingType | 
try {
    val result : PagedResponsePersonView = apiInstance.listPersons(limit, pageCursor, searchTitleOnly, searchTerm, sort, type)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PersonApi#listPersons")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PersonApi#listPersons")
    e.printStackTrace()
}
```

### Parameters
| **limit** | **kotlin.Double**|  | [optional] |
| **pageCursor** | **kotlin.String**|  | [optional] |
| **searchTitleOnly** | **kotlin.Boolean**|  | [optional] |
| **searchTerm** | **kotlin.String**|  | [optional] |
| **sort** | [**PersonSortType**](.md)|  | [optional] [enum: new, old, post_score, comment_score] |
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **type** | [**PersonListingType**](.md)|  | [optional] [enum: all, local] |

### Return type

[**PagedResponsePersonView**](PagedResponsePersonView.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a id="notePerson"></a>
# **notePerson**
> SuccessResponse notePerson(notePerson)

Make a note for a person.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PersonApi()
val notePerson : NotePerson =  // NotePerson | 
try {
    val result : SuccessResponse = apiInstance.notePerson(notePerson)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PersonApi#notePerson")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PersonApi#notePerson")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **notePerson** | [**NotePerson**](NotePerson.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a id="userBlockInstancePersons"></a>
# **userBlockInstancePersons**
> SuccessResponse userBlockInstancePersons(userBlockInstancePersonsParams)

Block an instance&#39;s persons as a user.

### Example
```kotlin
// Import classes:
//import com.idunnololz.summit.api.dto.lemmy.v4.infrastructure.*
//import com.idunnololz.summit.api.dto.lemmy.v4.models.*

val apiInstance = PersonApi()
val userBlockInstancePersonsParams : UserBlockInstancePersonsParams =  // UserBlockInstancePersonsParams | 
try {
    val result : SuccessResponse = apiInstance.userBlockInstancePersons(userBlockInstancePersonsParams)
    println(result)
} catch (e: ClientException) {
    println("4xx response calling PersonApi#userBlockInstancePersons")
    e.printStackTrace()
} catch (e: ServerException) {
    println("5xx response calling PersonApi#userBlockInstancePersons")
    e.printStackTrace()
}
```

### Parameters
| Name | Type | Description  | Notes |
| ------------- | ------------- | ------------- | ------------- |
| **userBlockInstancePersonsParams** | [**UserBlockInstancePersonsParams**](UserBlockInstancePersonsParams.md)|  | |

### Return type

[**SuccessResponse**](SuccessResponse.md)

### Authorization


Configure bearerAuth:
    ApiClient.accessToken = ""

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

