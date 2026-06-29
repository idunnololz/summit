
# ReportCombinedViewAnyOf1

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **tags** | [**kotlin.collections.List&lt;CommunityTag&gt;**](CommunityTag.md) | We wrap this in a struct so we can implement FromSqlRow&lt;Json&gt; for it |  |
| **creatorBannedFromCommunity** | **kotlin.Boolean** |  |  |
| **creatorBanned** | **kotlin.Boolean** |  |  |
| **creatorIsModerator** | **kotlin.Boolean** |  |  |
| **creatorIsAdmin** | **kotlin.Boolean** |  |  |
| **commentCreator** | [**Person**](Person.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **community** | [**Community**](Community.md) |  |  |
| **post** | [**Post**](Post.md) |  |  |
| **comment** | [**Comment**](Comment.md) |  |  |
| **commentReport** | [**CommentReport**](CommentReport.md) |  |  |
| **type** | [**inline**](#Type) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **resolver** | [**Person**](Person.md) |  |  [optional] |
| **commentActions** | [**CommentActions**](CommentActions.md) |  |  [optional] |


<a id="Type"></a>
## Enum: type_
| Name | Value |
| ---- | ----- |
| type | comment |



