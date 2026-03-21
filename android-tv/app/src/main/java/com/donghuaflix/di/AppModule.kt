package com.donghuaflix.di

import android.content.Context
import androidx.room.Room
import com.donghuaflix.BuildConfig
import com.donghuaflix.data.local.AppDatabase
import com.donghuaflix.data.local.dao.ShowDao
import com.donghuaflix.data.local.dao.SyncMetadataDao
import com.donghuaflix.data.local.dao.WatchHistoryDao
import com.donghuaflix.data.local.dao.WatchlistDao
import com.donghuaflix.data.remote.DonghuaApi
import coil.ImageLoader
import coil.disk.DiskCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDonghuaApi(retrofit: Retrofit): DonghuaApi {
        return retrofit.create(DonghuaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "donghuaflix.db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideShowDao(db: AppDatabase): ShowDao = db.showDao()
    @Provides fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao = db.watchHistoryDao()
    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideSyncMetadataDao(db: AppDatabase): SyncMetadataDao = db.syncMetadataDao()

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
