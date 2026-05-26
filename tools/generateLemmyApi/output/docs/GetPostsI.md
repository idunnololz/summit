
# GetPostsI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **searchUrlOnly** | **kotlin.Boolean** |  |  [optional] |
| **searchTitleOnly** | **kotlin.Boolean** |  |  [optional] |
| **searchTerm** | **kotlin.String** |  |  [optional] |
| **noCommentsOnly** | **kotlin.Boolean** | If true, then only show posts with no comments |  [optional] |
| **markAsRead** | **kotlin.Boolean** | Whether to automatically mark fetched posts as read. |  [optional] |
| **hideMedia** | **kotlin.Boolean** | If false, then show posts with media attached (even if your user setting is to hide them) |  [optional] |
| **showNsfw** | **kotlin.Boolean** | If true, then show the nsfw posts (even if your user setting is to hide them) |  [optional] |
| **showRead** | **kotlin.Boolean** | If true, then show the read posts (even if your user setting is to hide them) |  [optional] |
| **showHidden** | **kotlin.Boolean** |  |  [optional] |
| **multiCommunityName** | **kotlin.String** |  |  [optional] |
| **multiCommunityId** | **kotlin.Double** |  |  [optional] |
| **creatorUsername** | **kotlin.String** |  |  [optional] |
| **creatorId** | **kotlin.Double** | The person id. |  [optional] |
| **communityName** | **kotlin.String** |  |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |
| **timeRangeSeconds** | **kotlin.Double** | Filter to within a given time range, in seconds. IE 60 would give results for the past minute. Use Zero to override the local_site and local_user time_range. |  [optional] |
| **sort** | [**PostSortType**](PostSortType.md) |  |  [optional] |
| **type** | [**ListingType**](ListingType.md) |  |  [optional] |



