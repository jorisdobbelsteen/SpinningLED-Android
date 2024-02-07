package nl.joris2k.spinningled.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import nl.joris2k.spinningled.MyAppBar
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
            Text(if (connected) "Connected" else "Disconnected")
            Text("${currentProgram.toString()} (${currentProgram.value})")
        }
    }
}
