package nl.joris2k.spinningled.viewmodel

import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import nl.joris2k.spinningled.repository.DiscoveryRepository
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository
) : ViewModel() {
    val services : StateFlow<List<NsdServiceInfo>> get() = discoveryRepository.services
}
