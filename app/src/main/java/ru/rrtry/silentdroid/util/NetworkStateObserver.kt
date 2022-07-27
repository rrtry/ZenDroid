package ru.rrtry.silentdroid.util

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

class NetworkStateObserver(contextReference: WeakReference<Context>):
    DefaultLifecycleObserver,
    ConnectivityManager.NetworkCallback() {

    interface NetworkCallback {

        fun onNetworkAvailable()

        fun onNetworkLost()
    }

    private lateinit var connectivityManager: ConnectivityManager
    private val context: Context = contextReference.get()!!

    private val listener: NetworkCallback
    get() = context as NetworkCallback

    private var isConnected: Boolean = true

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        isConnected = isNetworkValidated(connectivityManager.activeNetwork)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        connectivityManager.registerNetworkCallback(getNetworkRequest(), this)
        if (!isConnected) {
            listener.onNetworkLost()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        connectivityManager.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        checkConnectivityState()
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        checkConnectivityState()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        checkConnectivityState()
    }

    private fun isNetworkValidated(network: Network?): Boolean {
        connectivityManager.getNetworkCapabilities(network)?.apply {
            return hasCapability(NET_CAPABILITY_INTERNET) &&
                    hasCapability(NET_CAPABILITY_VALIDATED)
        }
        return false
    }

    private fun notifyNetworkStateChanged(valid: Boolean) {
        if (!isConnected && valid) {
            listener.onNetworkAvailable()
        } else if (isConnected && !valid) {
            listener.onNetworkLost()
        }
    }

    @Suppress("deprecation")
    private fun checkConnectivityState() {

        var networkValidated = false

        connectivityManager.allNetworks.forEach {
            if (!networkValidated) {
                networkValidated = isNetworkValidated(it)
            }
        }

        notifyNetworkStateChanged(networkValidated)
        isConnected = networkValidated
    }

    companion object {

        private fun getNetworkRequest(): NetworkRequest {
            return NetworkRequest.Builder()
                .addCapability(NET_CAPABILITY_INTERNET)
                .addCapability(NET_CAPABILITY_VALIDATED)
                .addTransportType(TRANSPORT_WIFI)
                .addTransportType(TRANSPORT_VPN)
                .addTransportType(TRANSPORT_ETHERNET)
                .addTransportType(TRANSPORT_CELLULAR)
                .build()
        }
    }
}