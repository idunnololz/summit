
# EditSite

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **imageUploadDisabled** | **kotlin.Boolean** |  |  [optional] |
| **imageAllowVideoUploads** | **kotlin.Boolean** |  |  [optional] |
| **imageMaxUploadSize** | **kotlin.Double** |  |  [optional] |
| **imageMaxBannerSize** | **kotlin.Double** |  |  [optional] |
| **imageMaxAvatarSize** | **kotlin.Double** |  |  [optional] |
| **imageMaxThumbnailSize** | **kotlin.Double** |  |  [optional] |
| **imageUploadTimeoutSeconds** | **kotlin.Double** |  |  [optional] |
| **imageProxyBypassDomains** | **kotlin.String** | Allows bypassing proxy for specific image hosts when using [[ImageMode.ProxyAllImages]]. Use a comma-delimited string.  Example: i.imgur.com,postimg.cc |  [optional] |
| **imageMode** | [**ImageMode**](ImageMode.md) |  |  [optional] |
| **suggestedMultiCommunityId** | **kotlin.Double** |  |  [optional] |
| **emailNotificationsDisabled** | **kotlin.Boolean** | Dont send email notifications to users for new replies, mentions etc |  [optional] |
| **nsfwContentDisallowed** | **kotlin.Boolean** | Block NSFW content being created |  [optional] |
| **commentDownvotes** | [**FederationMode**](FederationMode.md) |  |  [optional] |
| **commentUpvotes** | [**FederationMode**](FederationMode.md) |  |  [optional] |
| **postDownvotes** | [**FederationMode**](FederationMode.md) |  |  [optional] |
| **postUpvotes** | [**FederationMode**](FederationMode.md) |  |  [optional] |
| **oauthRegistration** | **kotlin.Boolean** | Whether or not external auth methods can auto-register users. |  [optional] |
| **contentWarning** | **kotlin.String** | If present, nsfw content is visible by default. Should be displayed by frontends/clients when the site is first opened by a user. |  [optional] |
| **reportsEmailAdmins** | **kotlin.Boolean** | Whether to email admins for new reports. |  [optional] |
| **registrationMode** | [**RegistrationMode**](RegistrationMode.md) |  |  [optional] |
| **blockedUrls** | **kotlin.collections.List&lt;kotlin.String&gt;** | A list of blocked URLs |  [optional] |
| **federationEnabled** | **kotlin.Boolean** | Whether to enable federation. |  [optional] |
| **rateLimitImportUserSettingsIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitImportUserSettingsMaxRequests** | **kotlin.Double** | The number of settings imports or exports allowed in a given time frame. |  [optional] |
| **rateLimitSearchIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitSearchMaxRequests** | **kotlin.Double** | The number of searches allowed in a given time frame. |  [optional] |
| **rateLimitCommentIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitCommentMaxRequests** | **kotlin.Double** | The number of comments allowed in a given time frame. |  [optional] |
| **rateLimitImageIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitImageMaxRequests** | **kotlin.Double** | The number of image uploads allowed in a given time frame. |  [optional] |
| **rateLimitRegisterIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitRegisterMaxRequests** | **kotlin.Double** | The number of registrations allowed in a given time frame. |  [optional] |
| **rateLimitPostIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitPostMaxRequests** | **kotlin.Double** | The number of posts allowed in a given time frame. |  [optional] |
| **rateLimitMessageIntervalSeconds** | **kotlin.Double** |  |  [optional] |
| **rateLimitMessageMaxRequests** | **kotlin.Double** | The number of messages allowed in a given time frame. |  [optional] |
| **slurFilterRegex** | **kotlin.String** | A regex string of items to filter. |  [optional] |
| **discussionLanguages** | **kotlin.collections.List&lt;kotlin.Double&gt;** | A list of allowed discussion languages. |  [optional] |
| **federationSignedFetch** | **kotlin.Boolean** | Whether to sign outgoing Activitypub fetches with private key of local instance. Some Fediverse instances and platforms require this. |  [optional] |
| **applicationEmailAdmins** | **kotlin.Boolean** | Whether to email admins when receiving a new application. |  [optional] |
| **legalInformation** | **kotlin.String** | An optional page of legal information |  [optional] |
| **defaultCommentSortType** | [**CommentSortType**](CommentSortType.md) |  |  [optional] |
| **defaultItemsPerPage** | **kotlin.Double** | A default fetch limit for number of items returned. |  [optional] |
| **defaultPostTimeRangeSeconds** | **kotlin.Double** | A default time range limit to apply to post sorts, in seconds. 0 means none. |  [optional] |
| **defaultPostSortType** | [**PostSortType**](PostSortType.md) |  |  [optional] |
| **defaultPostListingMode** | [**PostListingMode**](PostListingMode.md) |  |  [optional] |
| **defaultPostListingType** | [**ListingType**](ListingType.md) |  |  [optional] |
| **defaultTheme** | **kotlin.String** | The default theme. Usually \&quot;browser\&quot; |  [optional] |
| **privateInstance** | **kotlin.Boolean** | Whether your instance is public, or private. |  [optional] |
| **applicationQuestion** | **kotlin.String** | Your application question form. This is in markdown, and can be many questions. |  [optional] |
| **emailVerificationRequired** | **kotlin.Boolean** | Whether to require email verification. |  [optional] |
| **communityCreationAdminOnly** | **kotlin.Boolean** | Limits community creation to admins only. |  [optional] |
| **summary** | **kotlin.String** | A shorter, one line description of your site. |  [optional] |
| **sidebar** | **kotlin.String** | A sidebar for the site, in markdown. |  [optional] |
| **name** | **kotlin.String** |  |  [optional] |



