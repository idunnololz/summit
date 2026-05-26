
# ResolveObjectViewAnyOf

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **creatorBannedFromCommunity** | **kotlin.Boolean** |  |  |
| **creatorIsModerator** | **kotlin.Boolean** |  |  |
| **creatorBanned** | **kotlin.Boolean** |  |  |
| **canMod** | **kotlin.Boolean** |  |  |
| **tags** | [**kotlin.collections.List&lt;CommunityTag&gt;**](CommunityTag.md) | We wrap this in a struct so we can implement FromSqlRow&lt;Json&gt; for it |  |
| **creatorIsAdmin** | **kotlin.Boolean** |  |  |
| **community** | [**Community**](Community.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **post** | [**Post**](Post.md) |  |  |
| **type** | [**inline**](#Type) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **postActions** | [**PostActions**](PostActions.md) |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |
| **imageDetails** | [**ImageDetails**](ImageDetails.md) |  |  [optional] |


<a id="Type"></a>
## Enum: type_
| Name | Value |
| ---- | ----- |
| type | post |



