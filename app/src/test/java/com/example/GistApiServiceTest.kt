package com.example

import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.api.GistApiService
import com.example.data.remote.interceptor.GitHubAuthInterceptor
import com.example.data.remote.model.CreateGistFileRequest
import com.example.data.remote.model.CreateGistRequest
import com.example.data.remote.model.DeleteGistRequest
import com.example.data.remote.model.DeleteGistResponse
import com.example.data.remote.model.GistDeleteResponse
import com.example.data.remote.model.GistListResponse
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import com.example.di.NetworkModule
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GistApiServiceTest {

  private lateinit var configPrefs: ConfigPrefs
  private lateinit var capturedRequests: MutableList<Request>
  private lateinit var nextResponseProvider: (Request) -> Response
  private lateinit var apiService: GistApiService

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    configPrefs = ConfigPrefs(context)
    configPrefs.setGithubToken("ghp_test_authenticated_token_12345")

    capturedRequests = mutableListOf()
    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("[]".toResponseBody("application/json".toMediaType()))
        .build()
    }

    val mockNetworkInterceptor = Interceptor { chain ->
      val request = chain.request()
      capturedRequests.add(request)
      nextResponseProvider(request)
    }

    val authInterceptor = GitHubAuthInterceptor(configPrefs)

    val okHttpClient =
      OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(mockNetworkInterceptor)
        .build()

    val moshi = NetworkModule.provideMoshi()

    val retrofit =
      Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    apiService = NetworkModule.provideGistApiService(retrofit)
  }

  @Test
  fun listGists_sendsAuthenticatedGetRequest_andParsesResponse() = runTest {
    val mockJsonResponse =
      """
      [
        {
          "id": "gist_test_001",
          "description": "Authenticated test gist",
          "public": true,
          "owner": {
            "login": "octocat",
            "id": 1,
            "avatar_url": "https://github.com/images/error/octocat_happy.gif"
          },
          "files": {
            "hello.kt": {
              "filename": "hello.kt",
              "type": "text/plain",
              "language": "Kotlin",
              "raw_url": "https://gist.githubusercontent.com/raw/1",
              "size": 42,
              "content": "println(\"Hello World\")"
            }
          }
        }
      ]
      """
        .trimIndent()

    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(mockJsonResponse.toResponseBody("application/json".toMediaType()))
        .build()
    }

    val gists: GistListResponse = apiService.listGists(page = 1, perPage = 10)

    assertEquals(1, gists.size)
    val firstGist = gists.first()
    assertEquals("gist_test_001", firstGist.id)
    assertEquals("Authenticated test gist", firstGist.description)
    assertEquals(true, firstGist.isPublic)
    assertEquals("octocat", firstGist.owner?.login)
    assertEquals(1, firstGist.files?.size)
    assertEquals("hello.kt", firstGist.files?.get("hello.kt")?.filename)
    assertEquals("println(\"Hello World\")", firstGist.files?.get("hello.kt")?.content)

    val recordedRequest = capturedRequests.last()
    assertEquals("GET", recordedRequest.method)
    assertTrue(recordedRequest.url.toString().startsWith("https://api.github.com/gists"))
    assertEquals("1", recordedRequest.url.queryParameter("page"))
    assertEquals("10", recordedRequest.url.queryParameter("per_page"))
    assertEquals(
      "Bearer ghp_test_authenticated_token_12345",
      recordedRequest.header("Authorization")
    )
    assertEquals("application/vnd.github+json", recordedRequest.header("Accept"))
    assertEquals("2022-11-28", recordedRequest.header("X-GitHub-Api-Version"))
  }

  @Test
  fun getGists_alias_sendsQueryParameters() = runTest {
    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("[]".toResponseBody("application/json".toMediaType()))
        .build()
    }

    apiService.getGists(page = 2, perPage = 30, since = "2026-01-01T00:00:00Z")

    val recordedRequest = capturedRequests.last()
    assertEquals("GET", recordedRequest.method)
    assertEquals("2", recordedRequest.url.queryParameter("page"))
    assertEquals("30", recordedRequest.url.queryParameter("per_page"))
    assertEquals("2026-01-01T00:00:00Z", recordedRequest.url.queryParameter("since"))
  }

  @Test
  fun createGist_sendsAuthenticatedPostRequest_withCreationModel() = runTest {
    val mockJsonResponse =
      """
      {
        "id": "new_gist_999",
        "description": "Newly created gist",
        "public": false,
        "files": {
          "main.kt": {
            "filename": "main.kt",
            "content": "fun main() {}"
          }
        }
      }
      """
        .trimIndent()

    var recordedBodyString: String? = null

    nextResponseProvider = { request ->
      val buffer = okio.Buffer()
      request.body?.writeTo(buffer)
      recordedBodyString = buffer.readUtf8()

      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(201)
        .message("Created")
        .body(mockJsonResponse.toResponseBody("application/json".toMediaType()))
        .build()
    }

    val requestPayload: CreateGistRequest =
      CreateGistRequest(
        description = "Newly created gist",
        isPublic = false,
        files = mapOf("main.kt" to CreateGistFileRequest(content = "fun main() {}"))
      )

    val createdGist = apiService.createGist(requestPayload)

    assertEquals("new_gist_999", createdGist.id)
    assertEquals("Newly created gist", createdGist.description)
    assertEquals(false, createdGist.isPublic)

    val recordedRequest = capturedRequests.last()
    assertEquals("POST", recordedRequest.method)
    assertEquals("https://api.github.com/gists", recordedRequest.url.toString())
    assertEquals(
      "Bearer ghp_test_authenticated_token_12345",
      recordedRequest.header("Authorization")
    )

    assertNotNull(recordedBodyString)
    assertTrue(recordedBodyString!!.contains("\"description\":\"Newly created gist\""))
    assertTrue(recordedBodyString!!.contains("\"public\":false"))
    assertTrue(recordedBodyString!!.contains("\"content\":\"fun main() {}\""))
  }

  @Test
  fun deleteGist_sendsAuthenticatedDeleteRequest_returns204NoContent() = runTest {
    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(204)
        .message("No Content")
        .body("".toResponseBody(null))
        .build()
    }

    val response = apiService.deleteGist("gist_to_delete_456")

    assertTrue(response.isSuccessful)
    assertEquals(204, response.code())

    val recordedRequest = capturedRequests.last()
    assertEquals("DELETE", recordedRequest.method)
    assertEquals("https://api.github.com/gists/gist_to_delete_456", recordedRequest.url.toString())
    assertEquals(
      "Bearer ghp_test_authenticated_token_12345",
      recordedRequest.header("Authorization")
    )
  }

  @Test
  fun listPublicGists_sendsGetRequestToPublicEndpoint() = runTest {
    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("[]".toResponseBody("application/json".toMediaType()))
        .build()
    }

    val result = apiService.listPublicGists(page = 1, perPage = 5)
    assertTrue(result.isEmpty())

    val recordedRequest = capturedRequests.last()
    assertEquals("GET", recordedRequest.method)
    assertTrue(recordedRequest.url.toString().startsWith("https://api.github.com/gists/public"))
  }

  @Test
  fun getGistDetails_and_getGist_retrieveSingleGist() = runTest {
    val mockJsonResponse =
      """
      {
        "id": "detail_gist_789",
        "description": "Details description",
        "public": true
      }
      """
        .trimIndent()

    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(mockJsonResponse.toResponseBody("application/json".toMediaType()))
        .build()
    }

    val detailGist = apiService.getGistDetails("detail_gist_789")
    assertEquals("detail_gist_789", detailGist.id)
    assertEquals("Details description", detailGist.description)

    val singleGist = apiService.getGist("detail_gist_789")
    assertEquals("detail_gist_789", singleGist.id)
  }

  @Test
  fun updateGist_sendsPatchRequest() = runTest {
    val mockJsonResponse =
      """
      {
        "id": "update_gist_111",
        "description": "Updated description",
        "public": true
      }
      """
        .trimIndent()

    nextResponseProvider = { request ->
      Response.Builder()
        .request(request)
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(mockJsonResponse.toResponseBody("application/json".toMediaType()))
        .build()
    }

    val updateRequest = GistRequest(description = "Updated description")
    val updated = apiService.updateGist("update_gist_111", updateRequest)

    assertEquals("update_gist_111", updated.id)
    assertEquals("Updated description", updated.description)

    val recordedRequest = capturedRequests.last()
    assertEquals("PATCH", recordedRequest.method)
    assertEquals("https://api.github.com/gists/update_gist_111", recordedRequest.url.toString())
  }

  @Test
  fun models_serialization_and_deserialization_withMoshi() {
    val moshi = NetworkModule.provideMoshi()

    // Test Creation Model
    val createReq =
      CreateGistRequest(
        description = "Test Moshi Creation",
        isPublic = true,
        files = mapOf("test.kt" to CreateGistFileRequest("val x = 1"))
      )
    val createAdapter = moshi.adapter(GistRequest::class.java)
    val createJson = createAdapter.toJson(createReq)
    val parsedCreateReq = createAdapter.fromJson(createJson)
    assertEquals(createReq.description, parsedCreateReq?.description)
    assertEquals(createReq.isPublic, parsedCreateReq?.isPublic)
    assertEquals(
      createReq.files["test.kt"]?.content,
      parsedCreateReq?.files?.get("test.kt")?.content
    )

    // Test Deletion Model
    val deleteResp: DeleteGistResponse =
      GistDeleteResponse(id = "del_123", success = true, message = "Deleted")
    val deleteAdapter = moshi.adapter(GistDeleteResponse::class.java)
    val deleteJson = deleteAdapter.toJson(deleteResp)
    val parsedDeleteResp = deleteAdapter.fromJson(deleteJson)
    assertEquals("del_123", parsedDeleteResp?.id)
    assertEquals(true, parsedDeleteResp?.success)

    // Test Deletion Request Model
    val deleteReq = DeleteGistRequest(id = "del_456")
    val deleteReqAdapter = moshi.adapter(DeleteGistRequest::class.java)
    val deleteReqJson = deleteReqAdapter.toJson(deleteReq)
    val parsedDeleteReq = deleteReqAdapter.fromJson(deleteReqJson)
    assertEquals("del_456", parsedDeleteReq?.id)

    // Test Listing Response Model
    val gistResp = GistResponse(id = "g_001", description = "Listed Gist")
    val respAdapter = moshi.adapter(GistResponse::class.java)
    val respJson = respAdapter.toJson(gistResp)
    val parsedResp = respAdapter.fromJson(respJson)
    assertEquals("g_001", parsedResp?.id)
    assertEquals("Listed Gist", parsedResp?.description)
  }
}
