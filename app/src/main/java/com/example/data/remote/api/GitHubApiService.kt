package com.example.data.remote.api

import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import com.example.data.remote.model.GistOwnerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface mapping to the GitHub Gist API v3 endpoints.
 * Standard request headers (Accept, X-GitHub-Api-Version, Authorization) are
 * automatically injected by [com.example.data.remote.interceptor.GitHubAuthInterceptor].
 */
interface GitHubApiService {

    @GET("gists")
    suspend fun getGists(): List<GistResponse>

    @GET("gists/{id}")
    suspend fun getGist(
        @Path("id") id: String
    ): GistResponse

    @GET("user")
    suspend fun getAuthenticatedUser(): GistOwnerResponse

    @POST("gists")
    suspend fun createGist(
        @Body request: GistRequest
    ): GistResponse

    @PATCH("gists/{id}")
    suspend fun updateGist(
        @Path("id") id: String,
        @Body request: GistRequest
    ): GistResponse

    @DELETE("gists/{id}")
    suspend fun deleteGist(
        @Path("id") id: String
    ): Response<Unit>
}
