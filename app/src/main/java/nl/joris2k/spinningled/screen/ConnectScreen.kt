package nl.joris2k.spinningled.screen

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import nl.joris2k.spinningled.MyAppBar
import nl.joris2k.spinningled.R
import nl.joris2k.spinningled.viewmodel.DiscoveryViewModel

@Composable
private fun ConnectList(
    services: List<NsdServiceInfo>,
    onDeviceClick: (serviceName: String) -> Unit,
) {
    if (services.isEmpty()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(1f)) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 20.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(64.dp)
                        .height(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(stringResource(R.string.discovery_in_progress), style = MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        Column {
            services.forEach { service ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(service.serviceName)
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { onDeviceClick(service.serviceName) }) {
                        Text(stringResource(R.string.connect_button))
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectScreen(
    navController: NavHostController,
    onDeviceClick: (serviceName: String) -> Unit,
    discoveryViewModel: DiscoveryViewModel = hiltViewModel(),
) {
    MyAppBar(
        stringResource(R.string.connect_to_device),
        navController,
        showBackButton = false
    ) {
        innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val servicesState by discoveryViewModel.services.collectAsState()
            ConnectList(servicesState,
                onDeviceClick)
        }
    }
}
