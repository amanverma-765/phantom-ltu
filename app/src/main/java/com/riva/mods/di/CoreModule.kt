package com.riva.mods.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.riva.mods.BuildConfig
import com.riva.mods.core.auth.TokenProvider
import com.riva.mods.core.auth.TokenProviderImpl
import com.riva.mods.core.network.KtorClientFactory
import com.riva.mods.core.storage.TokenManager
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "riva_mods_prefs")

val PUBLIC_CLIENT = named("publicClient")
val REFRESH_CLIENT = named("refreshClient")
val SECURE_CLIENT = named("secureClient")

val coreModule = module {
    // DataStore
    single<DataStore<Preferences>> { androidContext().dataStore }

    // Ktor Client Factory
    single { KtorClientFactory() }

    // Token Manager
    single { TokenManager(get()) }

    // Public client - no auth, used for login/register
    single<HttpClient>(PUBLIC_CLIENT) {
        get<KtorClientFactory>().createPublicClient()
    }

    // Refresh client - no auth interceptor, used only for refresh endpoint
    single<HttpClient>(REFRESH_CLIENT) {
        get<KtorClientFactory>().createPublicClient()
    }

    // Token Provider - uses refresh client for token refresh
    single<TokenProvider> {
        TokenProviderImpl(
            tokenManager = get(),
            authClient = get(REFRESH_CLIENT),
            baseUrl = BuildConfig.BASE_URL
        )
    }

    // Secure client - has auth interceptor with automatic refresh
    single<HttpClient>(SECURE_CLIENT) {
        get<KtorClientFactory>().createAuthenticatedClient(
            tokenProvider = get()
        )
    }

    // Default HttpClient - use secure client
    single<HttpClient> {
        get(SECURE_CLIENT)
    }
}
