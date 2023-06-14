package com.idunnololz.summit.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route


class AccessTokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val authManager = RedditAuthManager.instance
        val accessToken = authManager.authToken

        if (!isRequestWithAccessToken(response) || accessToken == null) {
            return null
        }
        synchronized(this) {
            val newAccessToken = authManager.authToken
            // Access token is refreshed in another thread.
            if (!accessToken.equals(newAccessToken) && newAccessToken != null) {
                return newRequestWithAccessToken(response.request, newAccessToken);
            }

            // Need to refresh an access token
            val updatedAccessToken = authManager.refreshAccessToken()
            if (updatedAccessToken == null) {
                return null
            }
            return newRequestWithAccessToken(response.request, updatedAccessToken);
        }
    }

    private fun isRequestWithAccessToken(response: Response): Boolean {
        val header = response.request.header("Authorization")
        return header != null && header.startsWith("Bearer")
    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }
}