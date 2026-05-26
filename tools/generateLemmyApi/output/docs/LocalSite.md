
# LocalSite

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **imageUploadDisabled** | **kotlin.Boolean** |  |  |
| **imageAllowVideoUploads** | **kotlin.Boolean** | This affects post and comment images, but not avatars and banners. |  |
| **imageMaxUploadSize** | **kotlin.Double** | This affects post and comment images, but not avatar and banner sizes. |  |
| **imageMaxBannerSize** | **kotlin.Double** |  |  |
| **imageMaxAvatarSize** | **kotlin.Double** |  |  |
| **imageMaxThumbnailSize** | **kotlin.Double** | These are pixel sizes. Larger images are automatically downscaled. |  |
| **imageUploadTimeoutSeconds** | **kotlin.Double** |  |  |
| **imageMode** | [**ImageMode**](ImageMode.md) |  |  |
| **defaultItemsPerPage** | **kotlin.Double** |  |  |
| **emailNotificationsDisabled** | **kotlin.Boolean** | Dont send email notifications to users for new replies, mentions etc |  |
| **usersActiveHalfYear** | **kotlin.Double** | The number of users with any activity in the last half year. |  |
| **usersActiveMonth** | **kotlin.Double** | The number of users with any activity in the last month. |  |
| **usersActiveWeek** | **kotlin.Double** | The number of users with any activity in the last week. |  |
| **usersActiveDay** | **kotlin.Double** | The number of users with any activity in the last day. |  |
| **communities** | **kotlin.Double** |  |  |
| **comments** | **kotlin.Double** |  |  |
| **posts** | **kotlin.Double** |  |  |
| **users** | **kotlin.Double** |  |  |
| **nsfwContentDisallowed** | **kotlin.Boolean** | Block NSFW content being created |  |
| **commentDownvotes** | [**FederationMode**](FederationMode.md) |  |  |
| **commentUpvotes** | [**FederationMode**](FederationMode.md) |  |  |
| **postDownvotes** | [**FederationMode**](FederationMode.md) |  |  |
| **postUpvotes** | [**FederationMode**](FederationMode.md) |  |  |
| **oauthRegistration** | **kotlin.Boolean** | Whether or not external auth methods can auto-register users. |  |
| **defaultCommentSortType** | [**CommentSortType**](CommentSortType.md) |  |  |
| **defaultPostSortType** | [**PostSortType**](PostSortType.md) |  |  |
| **defaultPostListingMode** | [**PostListingMode**](PostListingMode.md) |  |  |
| **federationSignedFetch** | **kotlin.Boolean** | Whether to sign outgoing Activitypub fetches with private key of local instance. Some Fediverse instances and platforms require this. |  |
| **reportsEmailAdmins** | **kotlin.Boolean** | Whether to email admins on new reports. |  |
| **registrationMode** | [**RegistrationMode**](RegistrationMode.md) |  |  |
| **publishedAt** | **kotlin.String** |  |  |
| **federationEnabled** | **kotlin.Boolean** | Whether federation is enabled. |  |
| **applicationEmailAdmins** | **kotlin.Boolean** | Whether new applications email admins. |  |
| **defaultPostListingType** | [**ListingType**](ListingType.md) |  |  |
| **defaultTheme** | **kotlin.String** | The default front-end theme. |  |
| **privateInstance** | **kotlin.Boolean** | Whether the instance is private or public. |  |
| **emailVerificationRequired** | **kotlin.Boolean** | Whether emails are required. |  |
| **communityCreationAdminOnly** | **kotlin.Boolean** | Whether only admins can create communities. |  |
| **siteSetup** | **kotlin.Boolean** | True if the site is set up. |  |
| **siteId** | **kotlin.Double** | The site id. |  |
| **id** | **kotlin.Double** | The local site id. |  |
| **imageProxyBypassDomains** | **kotlin.String** | Allows bypassing proxy for specific image hosts when using [[ImageMode.ProxyAllImages]]. Use a comma-delimited string.  Example: i.imgur.com,postimg.cc |  [optional] |
| **suggestedMultiCommunityId** | **kotlin.Double** |  |  [optional] |
| **defaultPostTimeRangeSeconds** | **kotlin.Double** | A default time range limit to apply to post sorts, in seconds. |  [optional] |
| **updatedAt** | **kotlin.String** |  |  [optional] |
| **slurFilterRegex** | **kotlin.String** | An optional regex to filter words. |  [optional] |
| **legalInformation** | **kotlin.String** | An optional legal disclaimer page. |  [optional] |
| **applicationQuestion** | **kotlin.String** | An optional registration application questionnaire in markdown. |  [optional] |



