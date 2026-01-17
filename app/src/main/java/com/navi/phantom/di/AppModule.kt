package com.navi.phantom.di

import com.navi.phantom.data.apps.DeviceAppProviderImpl
import com.navi.phantom.data.patcher.PatcherProviderImpl
import com.navi.phantom.data.installer.ApkInstallationProviderImpl
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.domain.repository.PatcherProvider
import com.navi.phantom.domain.repository.DeviceAppProvider
import com.navi.phantom.domain.usecase.PatcherUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.InstallationUseCase
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::DeviceAppProviderImpl) { bind<DeviceAppProvider>() }
    singleOf(::PatcherProviderImpl) { bind<PatcherProvider>() }
    singleOf(::ApkInstallationProviderImpl) { bind<ApkInstallationProvider>() }

    singleOf(::DeviceAppUseCase)
    singleOf(::PatcherUseCase)
    singleOf(::InstallationUseCase)

    viewModelOf(::AppViewModel)
    viewModelOf(::PatcherViewModel)
}
