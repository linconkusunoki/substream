package com.example.substream.di

import com.example.substream.BuildConfig
import com.example.substream.data.api.ApiClientFactory
import com.example.substream.data.repository.SubsonicRepository
import com.example.substream.player.PlayerManager
import com.example.substream.ui.screens.AlbumDetailViewModel
import com.example.substream.ui.screens.AlbumsViewModel
import com.example.substream.ui.screens.PingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module declaring all our app's dependencies.
 */
val appModule = module {

    // 1. single {} creates a Singleton (only one instance for the whole app)
    // Here we provide the Retrofit API Service
    single {
        ApiClientFactory.createService(BuildConfig.NAVIDROME_URL)
    }

    // 2. The get() function magically finds the dependency needed
    single {
        SubsonicRepository(api = get())
    }

    // PlayerManager initialized with Android Context
    single {
        PlayerManager(context = androidContext())
    }

    // 3. viewModel {} tells Koin how to build our ViewModels
    viewModel {
        PingViewModel(repository = get())
    }

    viewModel {
        AlbumsViewModel(repository = get())
    }

    viewModel {
        AlbumDetailViewModel(repository = get(), savedStateHandle = getOrNull())
    }
}
