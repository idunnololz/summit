
# GetModlogI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **bulkActionParentId** | **kotlin.Double** |  |  [optional] |
| **showBulk** | **kotlin.Boolean** | When &#x60;true&#x60; show all. When &#x60;false&#x60; or &#x60;None&#x60;, hide bulk actions (default). |  [optional] |
| **commentId** | **kotlin.Double** | The comment id. |  [optional] |
| **postId** | **kotlin.Double** | The post id. |  [optional] |
| **otherPersonId** | **kotlin.Double** | The person id. |  [optional] |
| **listingType** | [**ListingType**](ListingType.md) |  |  [optional] |
| **type** | [**ModlogKindFilter**](ModlogKindFilter.md) |  |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |
| **modPersonId** | **kotlin.Double** | The person id. |  [optional] |



