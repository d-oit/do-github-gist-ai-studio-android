package com.example.data.remote.api

import com.example.data.remote.model.GistResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface mapping to the public GitHub Gist API endpoints. Standard request headers are
 * automatically injected by the interceptor.
 */
interface GistApiService {

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
   * Retrieve individual gist details by ID.
   *
   * @param id The unique identifier of the gist.
   */
  @GET("gists/{id}") suspend fun getGistDetails(@Path("id") id: String): GistResponse
}
