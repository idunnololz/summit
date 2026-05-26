
# CommentView

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
| **post** | [**Post**](Post.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **comment** | [**Comment**](Comment.md) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **commentActions** | [**CommentActions**](CommentActions.md) |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |



