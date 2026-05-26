
# ListReportsI

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **myReportsOnly** | **kotlin.Boolean** | If true, view all your created reports. Works for non-admins/mods also. |  [optional] |
| **showCommunityRuleViolations** | **kotlin.Boolean** | Only for admins: also show reports with &#x60;violates_instance_rules&#x3D;false&#x60; |  [optional] |
| **limit** | **kotlin.Double** |  |  [optional] |
| **pageCursor** | **kotlin.String** | To get the next or previous page, pass this string unchanged as &#x60;page_cursor&#x60; in a new request to the same endpoint.  Do not attempt to parse or modify the cursor string. The format is internal and may change in minor Lemmy versions. |  [optional] |
| **communityId** | **kotlin.Double** | The community id. |  [optional] |
| **postId** | **kotlin.Double** | The post id. |  [optional] |
| **type** | [**ReportType**](ReportType.md) |  |  [optional] |
| **unresolvedOnly** | **kotlin.Boolean** | Only shows the unresolved reports |  [optional] |



