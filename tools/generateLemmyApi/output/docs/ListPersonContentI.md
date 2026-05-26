
# ListPersonContentI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **communityName** | **kotlin.String** |  |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |
| **username** | **kotlin.String** | Example: dessalines , or dessalines@xyz.tld |  [optional] |
| **personId** | **kotlin.Double** | The person id. |  [optional] |
| **type** | [**PersonContentType**](PersonContentType.md) |  |  [optional] |



