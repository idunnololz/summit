
# GetCommentsI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **searchTerm** | **kotlin.String** |  |  [optional] |
| **parentId** | **kotlin.Double** | The comment id. |  [optional] |
| **postId** | **kotlin.Double** | The post id. |  [optional] |
| **creatorUsername** | **kotlin.String** |  |  [optional] |
| **creatorId** | **kotlin.Double** | The person id. |  [optional] |
| **communityName** | **kotlin.String** |  |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **maxDepth** | **kotlin.Double** |  |  [optional] |
| **timeRangeSeconds** | **kotlin.Double** | Filter to within a given time range, in seconds. IE 60 would give results for the past minute. |  [optional] |
| **sort** | [**CommentSortType**](CommentSortType.md) |  |  [optional] |
| **type** | [**ListingType**](ListingType.md) |  |  [optional] |



