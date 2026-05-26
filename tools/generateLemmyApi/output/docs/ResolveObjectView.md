
# ResolveObjectView

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **type** | [**inline**](#Type) |  |  |
| **creatorBannedFromCommunity** | **kotlin.Boolean** |  |  |
| **creatorIsModerator** | **kotlin.Boolean** |  |  |
| **creatorBanned** | **kotlin.Boolean** |  |  |
| **canMod** | **kotlin.Boolean** |  |  |
| **tags** | [**kotlin.collections.List&lt;CommunityTag&gt;**](CommunityTag.md) | We wrap this in a struct so we can implement FromSqlRow&lt;Json&gt; for it |  |
| **creatorIsAdmin** | **kotlin.Boolean** |  |  |
| **community** | [**Community**](Community.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **post** | [**Post**](Post.md) |  |  |
| **comment** | [**Comment**](Comment.md) |  |  |
| **banned** | **kotlin.Boolean** |  |  |
| **isAdmin** | **kotlin.Boolean** |  |  |
| **person** | [**Person**](Person.md) |  |  |
| **owner** | [**Person**](Person.md) |  |  |
| **multi** | [**MultiCommunity**](MultiCommunity.md) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **postActions** | [**PostActions**](PostActions.md) |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |
| **imageDetails** | [**ImageDetails**](ImageDetails.md) |  |  [optional] |
| **commentActions** | [**CommentActions**](CommentActions.md) |  |  [optional] |
| **banExpiresAt** | **kotlin.String** |  |  [optional] |
| **followState** | [**CommunityFollowerState**](CommunityFollowerState.md) |  |  [optional] |


<a id="Type"></a>
## Enum: type_
| Name | Value |
| ---- | ----- |
| type | multi_community |



