package com.example.eldroid_nullpoint.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes internet connectivity and emits [true] when a validated internet
 * connection is available, [false] when the device goes offline.
 *
 * Spec §4.5: "Enter offline UI state. Display cached information as
 * 'Last updated …'. Clearly state availability may have changed.
 * Disable actions requiring live backend validation.
 * Automatically refresh after reconnection."
 */
object NetworkMonitor {

    /** Synchronous check — true when internet is believed to be available right now. */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Cold Flow that emits the current online state and every subsequent change.
     * Collect on a background dispatcher; switch to Main to update UI.
     */
    fun observe(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities
            ) {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(validated)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)

        // Emit current state immediately so collectors get an initial value
        trySend(isOnline(context))

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
