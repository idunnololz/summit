
# SearchResponse

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **multiCommunities** | [**kotlin.collections.List&lt;MultiCommunityView&gt;**](MultiCommunityView.md) |  |  |
| **persons** | [**kotlin.collections.List&lt;PersonView&gt;**](PersonView.md) |  |  |
| **communities** | [**kotlin.collections.List&lt;CommunityView&gt;**](CommunityView.md) |  |  |
| **posts** | [**kotlin.collections.List&lt;PostView&gt;**](PostView.md) |  |  |
| **comments** | [**kotlin.collections.List&lt;CommentView&gt;**](CommentView.md) |  |  |
| **nextPage** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **prevPage** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **resolve** | [**ResolveObjectView**](ResolveObjectView.md) |  |  [optional] |



