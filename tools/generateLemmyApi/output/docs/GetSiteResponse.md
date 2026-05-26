
# GetSiteResponse

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **captchaEnabled** | **kotlin.Boolean** |  |  |
| **activePlugins** | [**kotlin.collections.List&lt;PluginMetadata&gt;**](PluginMetadata.md) |  |  |
| **blockedUrls** | [**kotlin.collections.List&lt;LocalSiteUrlBlocklist&gt;**](LocalSiteUrlBlocklist.md) |  |  |
| **adminOauthProviders** | [**kotlin.collections.List&lt;AdminOAuthProvider&gt;**](AdminOAuthProvider.md) |  |  |
| **oauthProviders** | [**kotlin.collections.List&lt;PublicOAuthProvider&gt;**](PublicOAuthProvider.md) | A list of external auth methods your site supports. |  |
| **discussionLanguages** | **kotlin.collections.List&lt;kotlin.Double&gt;** |  |  |
| **allLanguages** | [**kotlin.collections.List&lt;Language&gt;**](Language.md) |  |  |
| **version** | **kotlin.String** |  |  |
| **admins** | [**kotlin.collections.List&lt;PersonView&gt;**](PersonView.md) |  |  |
| **siteView** | [**SiteView**](SiteView.md) |  |  |
| **lastApplicationDurationSeconds** | **kotlin.Double** | The number of seconds between the last application published, and approved / denied time.  Useful for estimating when your application will be approved. |  [optional] |
| **tagline** | [**Tagline**](Tagline.md) |  |  [optional] |



