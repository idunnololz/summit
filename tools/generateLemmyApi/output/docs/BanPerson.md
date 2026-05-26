
# BanPerson

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **reason** | **kotlin.String** |  |  |
| **ban** | **kotlin.Boolean** |  |  |
| **personId** | **kotlin.Double** | The person id. |  |
| **expiresAt** | **kotlin.Double** | A time that the ban will expire, in unix epoch seconds.  An i64 unix timestamp is used for a simpler API client implementation. |  [optional] |
| **removeOrRestoreData** | **kotlin.Boolean** | Optionally remove or restore all their data. Useful for new troll accounts. If ban is true, then this means remove. If ban is false, it means restore. |  [optional] |



