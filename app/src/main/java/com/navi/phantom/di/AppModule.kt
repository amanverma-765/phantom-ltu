package com.navi.phantom.di

import com.navi.phantom.data.apps.InstalledAppProviderImpl
import com.navi.phantom.data.apps.datasource.InstalledAppDataSource
import com.navi.phantom.data.bootstrap.BootstrapProviderImpl
import com.navi.phantom.data.installer.ApkInstallationProviderImpl
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.domain.repository.BootstrapProvider
import com.navi.phantom.domain.repository.InstalledAppProvider
import com.navi.phantom.domain.usecase.BootstrapUseCase
import com.navi.phantom.domain.usecase.InstalledAppUseCase
import com.navi.phantom.domain.usecase.InstallationUseCase
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.bootstrap.logic.BootstrapViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Data sources
    singleOf(::InstalledAppDataSource)
    
    // Providers (interface implementations)
    singleOf(::InstalledAppProviderImpl) { bind<InstalledAppProvider>() }
    singleOf(::BootstrapProviderImpl) { bind<BootstrapProvider>() }
    singleOf(::ApkInstallationProviderImpl) { bind<ApkInstallationProvider>() }

    // Use cases
    singleOf(::InstalledAppUseCase)
    single { BootstrapUseCase(androidContext(), get()) }
    single { InstallationUseCase(androidContext(), get()) }

    // ViewModels
    viewModel { AppViewModel(get()) }
    viewModel { BootstrapViewModel(androidApplication(), get(), get(), get()) }
}