@file:OptIn(UnstableApi::class)

package com.example.substream.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import com.example.substream.BuildConfig
import com.example.substream.data.api.ApiClientFactory
import com.example.substream.data.repository.SubsonicRepository
import com.example.substream.player.PlayerManager
import com.example.substream.player.cache.MediaCacheManager
import com.example.substream.ui.screens.AlbumDetailViewModel
import com.example.substream.ui.screens.AlbumsViewModel
import com.example.substream.ui.screens.LibraryViewModel
import com.example.substream.ui.screens.PingViewModel
import com.example.substream.ui.screens.SearchViewModel
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

    // 3. Media Cache Manager & DataSource Factory (Singleton instance)
    single {
        MediaCacheManager(context = androidContext(), cacheSizeMb = 500L)
    }

    single<CacheDataSource.Factory> {
        val mediaCacheManager = get<MediaCacheManager>()
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("SubStream/1.0")

        CacheDataSource.Factory()
            .setCache(mediaCacheManager.simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    // PlayerManager initialized with Android Context
    single {
        PlayerManager(context = androidContext())
    }

    // 4. viewModel {} tells Koin how to build our ViewModels
    viewModel {
        PingViewModel(repository = get())
    }

    viewModel {
        AlbumsViewModel(repository = get())
    }

    viewModel {
        AlbumDetailViewModel(repository = get(), savedStateHandle = getOrNull())
    }

    viewModel {
        SearchViewModel(repository = get())
    }

    viewModel {
        LibraryViewModel(repository = get())
    }
}
