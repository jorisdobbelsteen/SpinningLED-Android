package nl.joris2k.spinningled.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.joris2k.spinningled.device.AxisProgram
import nl.joris2k.spinningled.repository.DeviceRepository
import nl.joris2k.spinningled.repository.DiscoveryRepository
import timber.log.Timber
import java.net.SocketException
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deviceRepository : DeviceRepository,
    private val discoveryRepository: DiscoveryRepository
) : ViewModel() {
    private val hostname = savedStateHandle.get<String>("hostname")!!
    private val spinningLedDevice = deviceRepository.getDevice("$hostname.local")

    val connected : StateFlow<Boolean> get() = spinningLedDevice.connected
    val currentProgram : StateFlow<AxisProgram> get() = spinningLedDevice.currentProgram
    val screenSize : StateFlow<IntSize> get() = spinningLedDevice.screenSize

    init {
        reconnect()
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun reconnect() {
        viewModelScope.launch {
            try {
                spinningLedDevice.connect()
            } catch (e: UnresolvedAddressException) {
                Timber.w(e)
            } catch (e: SocketException) {
                Timber.w(e)
            }
        }
    }

    fun sendBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                spinningLedDevice.sendRgb565Bitmap(bitmap)
            } catch (e: Exception) {

            }
        }
    }
}