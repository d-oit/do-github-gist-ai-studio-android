package com.example

import androidx.compose.material3.SnackbarHostState
import com.example.data.repository.SyncStatus
import com.example.ui.util.SyncNotificationUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncNotificationUtilTest {

  @Test
  fun showSyncStatusSnackbar_showsSuccessMessage() = runBlocking {
    val hostState = SnackbarHostState()
    var dismissed = false

    val status = SyncStatus.Success("Synced 3 local changes", System.currentTimeMillis())

    val job = launch {
      SyncNotificationUtil.showSyncStatusSnackbar(
        snackbarHostState = hostState,
        syncStatus = status,
        onDismiss = { dismissed = true }
      )
    }
    yield()
    job.cancel()

    assertNotNull(status)
    assertEquals("Synced 3 local changes", status.message)
  }

  @Test
  fun showSyncStatusSnackbar_showsErrorMessage() = runBlocking {
    val hostState = SnackbarHostState()
    var dismissed = false

    val status = SyncStatus.Error("Connection reset by peer", System.currentTimeMillis())

    val job = launch {
      SyncNotificationUtil.showSyncStatusSnackbar(
        snackbarHostState = hostState,
        syncStatus = status,
        onDismiss = { dismissed = true }
      )
    }
    yield()
    job.cancel()

    assertNotNull(status)
    assertEquals("Connection reset by peer", status.errorMessage)
  }
}
