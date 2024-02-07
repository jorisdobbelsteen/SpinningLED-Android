package nl.joris2k.spinningled.repository

import nl.joris2k.spinningled.device.SpinningLedDevice
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor() {
    private val devices = WeakHashMap<String, SpinningLedDevice>()

    fun getDevice(hostname: String): SpinningLedDevice {
        return devices[hostname] ?: run {
            val device = SpinningLedDevice(hostname)
            devices[hostname] = device
            device
        }
    }
}
