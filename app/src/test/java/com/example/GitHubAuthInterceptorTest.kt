package com.example

import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.interceptor.GitHubAuthInterceptor
import java.util.concurrent.TimeUnit
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubAuthInterceptorTest {

  @Test
  fun interceptAddsStandardGithubHeadersAndAuthenticationToken() {
    val context = RuntimeEnvironment.getApplication()
    val configPrefs = ConfigPrefs(context)
    configPrefs.setGithubToken("my-secret-test-token")

    val interceptor = GitHubAuthInterceptor(configPrefs)

    val originalRequest = Request.Builder().url("https://api.github.com/gists").build()

    var interceptedRequest: Request? = null

    val fakeChain =
      object : Interceptor.Chain {
        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
          interceptedRequest = request
          return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(okhttp3.ResponseBody.create(null, ""))
            .build()
        }

        override fun connection(): Connection? = null

        override fun call(): okhttp3.Call = throw UnsupportedOperationException()

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
      }

    interceptor.intercept(fakeChain)

    val request = interceptedRequest!!
    assertEquals("application/vnd.github+json", request.header("Accept"))
    assertEquals("2022-11-28", request.header("X-GitHub-Api-Version"))
    assertEquals("Bearer my-secret-test-token", request.header("Authorization"))
  }

  @Test
  fun interceptDoesNotAddAuthorizationHeaderIfTokenIsEmpty() {
    val context = RuntimeEnvironment.getApplication()
    val configPrefs = ConfigPrefs(context)
    configPrefs.setGithubToken("")

    val interceptor = GitHubAuthInterceptor(configPrefs)

    val originalRequest = Request.Builder().url("https://api.github.com/gists").build()

    var interceptedRequest: Request? = null

    val fakeChain =
      object : Interceptor.Chain {
        override fun request(): Request = originalRequest

        override fun proceed(request: Request): Response {
          interceptedRequest = request
          return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(okhttp3.ResponseBody.create(null, ""))
            .build()
        }

        override fun connection(): Connection? = null

        override fun call(): okhttp3.Call = throw UnsupportedOperationException()

        override fun connectTimeoutMillis(): Int = 0

        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun readTimeoutMillis(): Int = 0

        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        override fun writeTimeoutMillis(): Int = 0

        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
      }

    interceptor.intercept(fakeChain)

    val request = interceptedRequest!!
    assertEquals("application/vnd.github+json", request.header("Accept"))
    assertEquals("2022-11-28", request.header("X-GitHub-Api-Version"))
    assertNull(request.header("Authorization"))
  }
}
