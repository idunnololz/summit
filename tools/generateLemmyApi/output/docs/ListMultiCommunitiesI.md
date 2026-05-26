
# ListMultiCommunitiesI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **searchTitleOnly** | **kotlin.Boolean** |  |  [optional] |
| **searchTerm** | **kotlin.String** |  |  [optional] |
| **timeRangeSeconds** | **kotlin.Double** | Filter to within a given time range, in seconds. IE 60 would give results for the past minute. |  [optional] |
| **creatorId** | **kotlin.Double** | The person id. |  [optional] |
| **sort** | [**MultiCommunitySortType**](MultiCommunitySortType.md) |  |  [optional] |
| **type** | [**MultiCommunityListingType**](MultiCommunityListingType.md) |  |  [optional] |



