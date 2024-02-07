package nl.joris2k.spinningled

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import nl.joris2k.spinningled.navigation.Screen
import nl.joris2k.spinningled.screen.ConnectScreen
import nl.joris2k.spinningled.screen.DeviceScreen
import nl.joris2k.spinningled.theme.SpinningLEDTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpinningLEDTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    MyNavHost(navController)
                }
            }
        }
    }
}

@Composable
fun MyNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Connect.route) {
        composable(Screen.Connect.route) {
            ConnectScreen(navController, onDeviceClick = { navController.navigate("device/${it}") })
        }
        composable(
            Screen.Device.route,
            arguments = Screen.Device.navArguments,
        ) {backStackEntry ->
            val hostname = backStackEntry.arguments?.getString("hostname") ?: ""
            DeviceScreen(navController, hostname)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SpinningLEDTheme {
        MyNavHost(rememberNavController())
    }
}
