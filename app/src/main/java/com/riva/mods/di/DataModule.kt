package com.riva.mods.di

import com.riva.mods.BuildConfig
import com.riva.mods.data.auth.remote.AuthApiService
import com.riva.mods.data.auth.repository.AuthRepositoryImpl
import com.riva.mods.domain.repository.AuthRepository
import org.koin.dsl.module

val dataModule = module {
    // Auth API Service - uses both clients
    single {
        AuthApiService(
            publicClient = get(PUBLIC_CLIENT),
            secureClient = get(SECURE_CLIENT),
            baseUrl = BuildConfig.BASE_URL
        )
    }

    // Auth Repository
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            tokenManager = get()
        )
    }
}
