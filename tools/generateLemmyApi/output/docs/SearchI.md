
# SearchI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **searchTerm** | **kotlin.String** | The search query. Can be a plain text, or an object ID which will be resolved (eg &#x60;https://lemmy.world/comment/1&#x60; or &#x60;!fediverse@lemmy.ml&#x60;). |  |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **showNsfw** | **kotlin.Boolean** | If true, then show the nsfw posts (even if your user setting is to hide them) |  [optional] |
| **postUrlOnly** | **kotlin.Boolean** |  |  [optional] |
| **titleOnly** | **kotlin.Boolean** |  |  [optional] |
| **listingType** | [**ListingType**](ListingType.md) |  |  [optional] |
| **timeRangeSeconds** | **kotlin.Double** | Filter to within a given time range, in seconds. IE 60 would give results for the past minute. |  [optional] |
| **type** | [**SearchType**](SearchType.md) |  |  [optional] |
| **creatorUsername** | **kotlin.String** |  |  [optional] |
| **creatorId** | **kotlin.Double** | The person id. |  [optional] |
| **communityName** | **kotlin.String** |  |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |



