package com.example.di

import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.interceptor.GitHubAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {

  fun provideMoshi(): Moshi {
    return Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  }

  fun provideOkHttpClient(
    authInterceptor: GitHubAuthInterceptor,
    context: android.content.Context
  ): OkHttpClient {
    val logging =
      HttpLoggingInterceptor { message ->
          val sanitized = com.example.core.security.PrivacySanitizer.redact(message)
          android.util.Log.d("OkHttp", sanitized)
        }
        .apply {
          level =
            if (com.example.BuildConfig.DEBUG) {
              HttpLoggingInterceptor.Level.BASIC
            } else {
              HttpLoggingInterceptor.Level.NONE
            }
        }
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

  fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
    return Retrofit.Builder()
      .baseUrl("https://api.github.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
  }

  fun provideGitHubApiService(retrofit: Retrofit): GitHubApiService {
    return retrofit.create(GitHubApiService::class.java)
  }
}
