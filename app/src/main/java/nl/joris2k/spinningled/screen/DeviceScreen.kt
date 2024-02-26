package nl.joris2k.spinningled.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import nl.joris2k.spinningled.MyAppBar
import nl.joris2k.spinningled.R
import nl.joris2k.spinningled.viewmodel.DeviceViewModel

@Composable
fun DeviceScreen(
    navController: NavHostController,
    hostname: String,
    deviceViewModel: DeviceViewModel = hiltViewModel(),
) {
    MyAppBar(hostname, navController) {
        innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            val connected by deviceViewModel.connected.collectAsState()
            val currentProgram by deviceViewModel.currentProgram.collectAsState()
            if (!connected) {
                Row(modifier = Modifier.fillMaxWidth(1.0f)) {
                    Text("Not connected to device")
                    OutlinedButton(onClick = { deviceViewModel.reconnect() }) {
                        Text(stringResource(R.string.reconnect_button))
                    }
                }
            }
            Text("${currentProgram.toString()} (${currentProgram.value})")
            CameraScreen(onBitmapCaptured = { bitmap ->
                deviceViewModel.sendBitmap(bitmap)
            })
        }
    }
}
