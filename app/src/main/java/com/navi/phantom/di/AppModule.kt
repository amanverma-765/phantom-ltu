package com.navi.phantom.di

import com.navi.phantom.data.bootstrap.ApkInstaller
import com.navi.phantom.data.bootstrap.BootstrapEngine
import com.navi.phantom.data.apps.datasource.InstalledAppDataSource
import com.navi.phantom.data.apps.repository.InstalledAppRepoImpl
import com.navi.phantom.domain.repository.InstalledAppRepository
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.bootstrap.logic.BootstrapViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Data sources
    singleOf(::InstalledAppDataSource)
    singleOf(::InstalledAppRepoImpl) {
        bind<InstalledAppRepository>()
    }

    // Bootstrap
    single { BootstrapEngine(androidContext()) }
    single { ApkInstaller(androidContext()) }

    // ViewModels
    viewModelOf(::AppViewModel)
    viewModel { BootstrapViewModel(androidApplication(), get(), get(), get()) }
}