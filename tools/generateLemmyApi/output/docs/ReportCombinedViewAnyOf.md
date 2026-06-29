
# ReportCombinedViewAnyOf

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **tags** | [**kotlin.collections.List&lt;CommunityTag&gt;**](CommunityTag.md) | We wrap this in a struct so we can implement FromSqlRow&lt;Json&gt; for it |  |
| **creatorBannedFromCommunity** | **kotlin.Boolean** |  |  |
| **creatorBanned** | **kotlin.Boolean** |  |  |
| **creatorIsModerator** | **kotlin.Boolean** |  |  |
| **creatorIsAdmin** | **kotlin.Boolean** |  |  |
| **postCreator** | [**Person**](Person.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **community** | [**Community**](Community.md) |  |  |
| **post** | [**Post**](Post.md) |  |  |
| **postReport** | [**PostReport**](PostReport.md) |  |  |
| **type** | [**inline**](#Type) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **resolver** | [**Person**](Person.md) |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **postActions** | [**PostActions**](PostActions.md) |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |


<a id="Type"></a>
## Enum: type_
| Name | Value |
| ---- | ----- |
| type | post |



