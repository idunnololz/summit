
# AdminOAuthProvider

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **usePkce** | **kotlin.Boolean** | switch to enable or disable PKCE |  |
| **publishedAt** | **kotlin.String** |  |  |
| **enabled** | **kotlin.Boolean** | switch to enable or disable an oauth provider |  |
| **accountLinkingEnabled** | **kotlin.Boolean** | Allows linking an OAUTH account to an existing user account by matching emails |  |
| **autoVerifyEmail** | **kotlin.Boolean** | Automatically sets email as verified on registration |  |
| **scopes** | **kotlin.String** | Lists the scopes requested from users. Users will have to grant access to the requested scope at sign up. |  |
| **clientId** | **kotlin.String** | The client_id is provided by the OAuth 2.0 provider and is a unique identifier to this service |  |
| **idClaim** | **kotlin.String** | The OAuth 2.0 claim containing the unique user ID returned by the provider. Usually this should be set to \&quot;sub\&quot;. |  |
| **userinfoEndpoint** | **kotlin.String** | The UserInfo Endpoint is an OAuth 2.0 Protected Resource that returns Claims about the authenticated End-User. This is defined in the OIDC specification. |  |
| **tokenEndpoint** | **kotlin.String** | The token endpoint is used by the client to obtain an access token by presenting its authorization grant or refresh token. This is usually provided by the OAUTH provider. |  |
| **authorizationEndpoint** | **kotlin.String** | The authorization endpoint is used to interact with the resource owner and obtain an authorization grant. This is usually provided by the OAUTH provider. |  |
| **issuer** | **kotlin.String** | The issuer url of the OAUTH provider. |  |
| **displayName** | **kotlin.String** | The OAuth 2.0 provider name displayed to the user on the Login page |  |
| **id** | **kotlin.Double** | The oauth provider id. |  |
| **updatedAt** | **kotlin.String** |  |  [optional] |



