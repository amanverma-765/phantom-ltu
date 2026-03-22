package com.navi.phantom.di

import com.navi.phantom.data.activelocation.ActiveLocationRepositoryImpl
import com.navi.phantom.data.apps.DeviceAppProviderImpl
import com.navi.phantom.data.patcher.PatcherProviderImpl
import com.navi.phantom.data.installer.ApkInstallationProviderImpl
import com.navi.phantom.data.places.PlaceRepositoryImpl
import com.navi.phantom.domain.repository.ActiveLocationRepository
import com.navi.phantom.domain.repository.ApkInstallationProvider
import com.navi.phantom.domain.repository.PatcherProvider
import com.navi.phantom.domain.repository.DeviceAppProvider
import com.navi.phantom.domain.repository.PlaceRepository
import com.navi.phantom.domain.usecase.ActiveLocationUseCase
import com.navi.phantom.domain.usecase.PatcherUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.InstallationUseCase
import com.navi.phantom.domain.usecase.PlaceUseCase
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.map.logic.MapPickerViewModel
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import com.navi.phantom.features.places.logic.PlaceSelectionViewModel
import com.navi.phantom.features.places.logic.PlacesViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::DeviceAppProviderImpl) { bind<DeviceAppProvider>() }
    singleOf(::PatcherProviderImpl) { bind<PatcherProvider>() }
    singleOf(::ApkInstallationProviderImpl) { bind<ApkInstallationProvider>() }
    singleOf(::PlaceRepositoryImpl) { bind<PlaceRepository>() }
    singleOf(::ActiveLocationRepositoryImpl) { bind<ActiveLocationRepository>() }

    singleOf(::DeviceAppUseCase)
    singleOf(::PatcherUseCase)
    singleOf(::InstallationUseCase)
    singleOf(::PlaceUseCase)
    singleOf(::ActiveLocationUseCase)

    viewModelOf(::AppViewModel)
    viewModelOf(::PatcherViewModel)
    viewModelOf(::PlacesViewModel)
    viewModelOf(::MapPickerViewModel)
    viewModelOf(::PlaceSelectionViewModel)
}
