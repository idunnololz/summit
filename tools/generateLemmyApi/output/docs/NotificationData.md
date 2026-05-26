
# NotificationData

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
| **post** | [**Post**](Post.md) |  |  |
| **creator** | [**Person**](Person.md) |  |  |
| **comment** | [**Comment**](Comment.md) |  |  |
| **recipient** | [**Person**](Person.md) |  |  |
| **privateMessage** | [**PrivateMessage**](PrivateMessage.md) |  |  |
| **modlog** | [**Modlog**](Modlog.md) |  |  |
| **creatorCommunityBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **creatorBanExpiresAt** | **kotlin.String** |  |  [optional] |
| **personActions** | [**PersonActions**](PersonActions.md) |  |  [optional] |
| **commentActions** | [**CommentActions**](CommentActions.md) |  |  [optional] |
| **communityActions** | [**CommunityActions**](CommunityActions.md) |  |  [optional] |
| **postActions** | [**PostActions**](PostActions.md) |  |  [optional] |
| **imageDetails** | [**ImageDetails**](ImageDetails.md) |  |  [optional] |
| **targetComment** | [**Comment**](Comment.md) |  |  [optional] |
| **targetPost** | [**Post**](Post.md) |  |  [optional] |
| **targetCommunity** | [**Community**](Community.md) |  |  [optional] |
| **targetInstance** | [**Instance**](Instance.md) |  |  [optional] |
| **targetPerson** | [**Person**](Person.md) |  |  [optional] |
| **moderator** | [**Person**](Person.md) |  |  [optional] |


<a id="Type"></a>
## Enum: type_
| Name | Value |
| ---- | ----- |
| type | mod_action |



