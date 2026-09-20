package com.example.data.remote.api

import com.example.data.remote.model.GistOwnerResponse
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface mapping to authenticated GitHub Gist API endpoints. Standard request
 * headers (Accept, X-GitHub-Api-Version, Authorization) are automatically injected by
 * [com.example.data.remote.interceptor.GitHubAuthInterceptor].
 */
interface GistApiService {

  /**
   * List gists for the authenticated user, or public gists if unauthenticated.
   *
   * @param page The page number of the results to retrieve.
   * @param perPage The number of results per page.
   * @param since Only gists updated after this time (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ).
   */
  @GET("gists")
  suspend fun listGists(
    @Query("page") page: Int? = null,
    @Query("per_page") perPage: Int? = null,
    @Query("since") since: String? = null
  ): List<GistResponse>

  /**
   * Alias for listing gists adhering to get naming convention.
   *
   * @param page The page number of the results to retrieve.
   * @param perPage The number of results per page.
   * @param since Only gists updated after this time (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ).
   */
  @GET("gists")
  suspend fun getGists(
    @Query("page") page: Int? = null,
    @Query("per_page") perPage: Int? = null,
    @Query("since") since: String? = null
  ): List<GistResponse>

  /**
   * List public gists.
   *
   * @param page The page number of the results to retrieve.
   * @param perPage The number of results per page.
   * @param since Only gists updated after this time (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ).
   */
  @GET("gists/public")
  suspend fun listPublicGists(
    @Query("page") page: Int? = null,
    @Query("per_page") perPage: Int? = null,
    @Query("since") since: String? = null
  ): List<GistResponse>

  /**
   * List starred gists for the authenticated user.
   *
   * @param page The page number of the results to retrieve.
   * @param perPage The number of results per page.
   * @param since Only gists updated after this time (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ).
   */
  @GET("gists/starred")
  suspend fun listStarredGists(
    @Query("page") page: Int? = null,
    @Query("per_page") perPage: Int? = null,
    @Query("since") since: String? = null
  ): List<GistResponse>

  /**
   * Retrieve individual gist details by ID.
   *
   * @param id The unique identifier of the gist.
   */
  @GET("gists/{id}") suspend fun getGistDetails(@Path("id") id: String): GistResponse

  /**
   * Retrieve individual gist details by ID (alias).
   *
   * @param id The unique identifier of the gist.
   */
  @GET("gists/{id}") suspend fun getGist(@Path("id") id: String): GistResponse

  /**
   * Create a new gist for the authenticated user.
   *
   * @param request The creation payload containing description, public status, and files.
   */
  @POST("gists") suspend fun createGist(@Body request: GistRequest): GistResponse

  /**
   * Update an existing gist.
   *
   * @param id The unique identifier of the gist.
   * @param request The update payload containing files and/or description.
   */
  @PATCH("gists/{id}")
  suspend fun updateGist(@Path("id") id: String, @Body request: GistRequest): GistResponse

  /**
   * Delete a gist by ID.
   *
   * @param id The unique identifier of the gist.
   */
  @DELETE("gists/{id}") suspend fun deleteGist(@Path("id") id: String): Response<Unit>

  /**
   * Retrieve a specific gist revision by commit SHA.
   *
   * @param id The unique identifier of the gist.
   * @param sha The commit SHA of the revision.
   */
  @GET("gists/{id}/{sha}")
  suspend fun getGistRevision(@Path("id") id: String, @Path("sha") sha: String): GistResponse

  /** Retrieve authenticated user profile information. */
  @GET("user") suspend fun getAuthenticatedUser(): GistOwnerResponse

  /**
   * Check if a gist is starred by the authenticated user.
   *
   * @param id The unique identifier of the gist.
   */
  @GET("gists/{id}/star") suspend fun checkIsStarred(@Path("id") id: String): Response<Unit>

  /**
   * Star a gist for the authenticated user.
   *
   * @param id The unique identifier of the gist.
   */
  @PUT("gists/{id}/star") suspend fun starGist(@Path("id") id: String): Response<Unit>

  /**
   * Unstar a gist for the authenticated user.
   *
   * @param id The unique identifier of the gist.
   */
  @DELETE("gists/{id}/star") suspend fun unstarGist(@Path("id") id: String): Response<Unit>

  /**
   * Fork a gist to the authenticated user's account.
   *
   * @param id The unique identifier of the gist.
   */
  @POST("gists/{id}/forks") suspend fun forkGist(@Path("id") id: String): GistResponse
}
