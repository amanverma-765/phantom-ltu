package com.riva.mods.di

import com.riva.mods.BuildConfig
import com.riva.mods.presentation.features.auth.AuthViewModel
import com.riva.mods.presentation.features.auth.GoogleSignInHelper
import com.riva.mods.presentation.features.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    single {
        GoogleSignInHelper(
            context = androidContext(),
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
    }

    viewModel {
        AuthViewModel(
            googleSignInHelper = get(),
            authRepository = get(),
            navigator = get()
        )
    }

    viewModel {
        HomeViewModel(authRepository = get())
    }
}
