package nl.joris2k.spinningled.repository

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class DiscoveryRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val nsdManager : NsdManager by lazy { context.getSystemService(NsdManager::class.java) }

    private val _services = callbackFlow<List<NsdServiceInfo>> {
        var servicesList =  listOf<NsdServiceInfo>()
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("Service discovery started")
                servicesList = emptyList()
                trySend(servicesList)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Service discovery start failed")
                servicesList = emptyList()
                trySend(servicesList)
                close() // TODO: Exception
            }

            override fun onDiscoveryStopped(serviceType: String) {
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.w("Service discovery stop failed")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                servicesList = servicesList + serviceInfo
                trySend(servicesList)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                servicesList = servicesList.filterNot { it.serviceName == serviceInfo.serviceName }
                trySend(servicesList)
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    val services : StateFlow<List<NsdServiceInfo>> = _services.stateIn(
        MainScope(),
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    suspend fun resolveService(service: NsdServiceInfo) = suspendCoroutine<NsdServiceInfo?> {
            continuation ->
        var callback = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                continuation.resume(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                continuation.resume(serviceInfo)
            }

        }
        nsdManager.resolveService(service, callback)
    }

    companion object {
        private val SERVICE_TYPE = "_spinningled._tcp."
    }
}