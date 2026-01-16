package com.navi.phantom.di

import com.navi.phantom.data.apps.DeviceAppProviderImpl
import com.navi.phantom.data.apps.datasource.DeviceAppDataSource
import com.navi.phantom.data.patcher.PatcherProviderImpl
import com.navi.phantom.data.installer.ApkInstallationProviderImpl
import com.navi.phantom.data.patchedapp.PatchedAppProviderImpl
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.domain.repository.PatcherProvider
import com.navi.phantom.domain.repository.DeviceAppProvider
import com.navi.phantom.domain.repository.PatchedAppProvider
import com.navi.phantom.domain.usecase.PatcherUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.InstallationUseCase
import com.navi.phantom.domain.usecase.PatchedAppUseCase
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    singleOf(::DeviceAppDataSource)

    singleOf(::DeviceAppProviderImpl) { bind<DeviceAppProvider>() }
    singleOf(::PatcherProviderImpl) { bind<PatcherProvider>() }
    singleOf(::ApkInstallationProviderImpl) { bind<ApkInstallationProvider>() }

    singleOf(::PatchedAppProviderImpl) { bind<PatchedAppProvider>() }

    singleOf(::DeviceAppUseCase)
    singleOf(::PatchedAppUseCase)
    single { PatcherUseCase(androidContext(), get()) }
    single { InstallationUseCase(androidContext(), get()) }

    viewModel { AppViewModel(get(), get()) }
    viewModel { PatcherViewModel(androidApplication(), get(), get(), get(), get()) }
}
