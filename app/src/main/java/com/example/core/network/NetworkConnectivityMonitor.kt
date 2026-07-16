package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.repository.GistRepository
import com.example.data.sync.GistSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A highly resilient network connectivity monitor that hooks into Android's reactive network
 * callback system and periodically verifies active internet access.
 *
 * When a connection is established (or during periodic health checks), it checks if the database
 * has any unsynchronized Gists and enqueues the synchronization worker.
 */
class NetworkConnectivityMonitor(
  private val context: Context,
  private val repository: GistRepository
) {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _isOnline = MutableStateFlow(false)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  companion object {
    private const val TAG = "NetworkMonitor"
    private const val CHECK_PERIOD_MS = 45_000L // 45 seconds period for internet check
  }

  init {
    _isOnline.value = isCurrentlyConnected()
    Log.d(TAG, "Network monitor initialized. Initial online state: ${_isOnline.value}")
  }

  /** Helper function to determine if the device currently has active Internet access. */
  fun isCurrentlyConnected(): Boolean {
    return try {
      val activeNetwork = connectivityManager.activeNetwork ?: return false
      val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
      Log.e(TAG, "Error checking active network status", e)
      false
    }
  }

  /**
   * Starts monitoring network connectivity reactively and periodically, executing the sync checks
   * when online.
   */
  fun startMonitoring(scope: CoroutineScope) {
    // 1. Hook: Register the NetworkCallback to monitor reactive network changes
    try {
      val request =
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()

      connectivityManager.registerNetworkCallback(
        request,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            val wasOnline = _isOnline.value
            _isOnline.value = true
            Log.d(TAG, "Network callback: Network is available. Was online: $wasOnline")
            if (!wasOnline) {
              triggerSyncIfPending(scope)
            }
          }

          override fun onLost(network: Network) {
            val currentlyOnline = isCurrentlyConnected()
            _isOnline.value = currentlyOnline
            Log.d(TAG, "Network callback: Network lost. Is still online: $currentlyOnline")
          }

          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
          ) {
            val hasInternet =
              networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val wasOnline = _isOnline.value
            _isOnline.value = hasInternet
            Log.d(
              TAG,
              "Network callback: Capabilities changed. Validated internet: $hasInternet. Was online: $wasOnline"
            )
            if (hasInternet && !wasOnline) {
              triggerSyncIfPending(scope)
            }
          }
        }
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to register network callback, relying on periodic checks", e)
    }

    // 2. Periodic Check: Background coroutine loop checking connectivity status periodically
    scope.launch(Dispatchers.IO) {
      while (true) {
        delay(CHECK_PERIOD_MS)
        try {
          val connected = isCurrentlyConnected()
          val wasOnline = _isOnline.value
          _isOnline.value = connected

          Log.d(TAG, "Periodic health check. Is online: $connected, Was online: $wasOnline")

          // Trigger sync if we are online (re-verify state/push any leftover unsynced gists)
          if (connected) {
            triggerSyncIfPending(this)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error in periodic network connectivity check", e)
        }
      }
    }
  }

  /** Checks if there are any pending local mutations and enqueues the synchronization task. */
  private fun triggerSyncIfPending(scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) {
      try {
        val unsyncedList = repository.getUnsynchronizedGists()
        if (unsyncedList.isNotEmpty()) {
          Log.d(
            TAG,
            "Internet connectivity verified. Found ${unsyncedList.size} unsynchronized Gist mutations. Triggering background sync..."
          )
          GistSyncWorker.enqueue(context)
        } else {
          Log.d(TAG, "Internet connectivity verified. No unsynchronized Gist mutations pending.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to check unsynchronized Gists on connectivity event", e)
      }
    }
  }
}
