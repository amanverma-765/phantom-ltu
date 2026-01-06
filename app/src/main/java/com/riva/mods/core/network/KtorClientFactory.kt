package com.riva.mods.core.network

import android.util.Log
import com.riva.mods.core.auth.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class KtorClientFactory {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        prettyPrint = true
        isLenient = true
    }

    fun createPublicClient(): HttpClient {
        return HttpClient(OkHttp) {
            installCommonPlugins()
        }
    }

    fun createAuthenticatedClient(tokenProvider: TokenProvider): HttpClient {
        return HttpClient(OkHttp) {
            installCommonPlugins()

            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = tokenProvider.getAccessToken()
                        val refreshToken = tokenProvider.getRefreshToken()

                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }

                    refreshTokens {
                        Log.d("KtorAuth", "401 received, refreshing tokens...")

                        val newAccessToken = tokenProvider.refreshTokens()
                        val newRefreshToken = tokenProvider.getRefreshToken()

                        if (newAccessToken != null && newRefreshToken != null) {
                            Log.d("KtorAuth", "Token refresh successful, retrying request")
                            BearerTokens(newAccessToken, newRefreshToken)
                        } else {
                            Log.w("KtorAuth", "Token refresh failed, request will fail")
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        // Send token for all requests EXCEPT public auth endpoints
                        val path = request.url.pathSegments.joinToString("/")
                        !path.endsWith("google/id-token") && !path.endsWith("refresh")
                    }
                }
            }
        }
    }

    private fun io.ktor.client.HttpClientConfig<*>.installCommonPlugins() {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

        install(HttpRedirect) {
            allowHttpsDowngrade = false
        }

        install(ContentNegotiation) {
            json(jsonConfig)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
        }

        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message)
                }
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
