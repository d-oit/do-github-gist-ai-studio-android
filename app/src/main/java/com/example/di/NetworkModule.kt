package com.example.di

import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.interceptor.GitHubAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideMoshi(): Moshi {
    return Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(
    authInterceptor: GitHubAuthInterceptor,
    @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
  ): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val cacheSize = 10 * 1024 * 1024L // 10 MB
    val cache = okhttp3.Cache(context.cacheDir, cacheSize)
    return OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(logging)
      .cache(cache)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  @Provides
  @Singleton
  fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService {
    return retrofit.create(GitHubApiService::class.java)
  }
}
