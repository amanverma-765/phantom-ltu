package com.navi.phantom.di

import com.navi.phantom.data.local.datasource.InstalledAppDataSource
import com.navi.phantom.data.local.repository.InstalledAppRepoImpl
import com.navi.phantom.domain.repository.InstalledAppRepository
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.bootstrap.logic.BootstrapViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::InstalledAppDataSource)
    singleOf(::InstalledAppRepoImpl) {
        bind<InstalledAppRepository>()
    }
    viewModelOf(::AppViewModel)
    viewModelOf(::BootstrapViewModel)
}
