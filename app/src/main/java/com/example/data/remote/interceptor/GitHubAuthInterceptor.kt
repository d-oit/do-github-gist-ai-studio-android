package com.example.data.remote.interceptor

import com.example.data.local.pref.ConfigPrefs
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An OkHttp Interceptor that dynamically adds the required GitHub API headers
 * and the Bearer token authorization header if configured.
 */
@Singleton
class GitHubAuthInterceptor @Inject constructor(
    private val configPrefs: ConfigPrefs
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // Automatically inject standard GitHub API version & Accept content-type headers
        builder.header("Accept", "application/vnd.github+json")
        builder.header("X-GitHub-Api-Version", "2022-11-28")

        // Automatically inject token if stored in prefs
        val token = configPrefs.getGithubToken().trim()
        if (token.isNotEmpty()) {
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
