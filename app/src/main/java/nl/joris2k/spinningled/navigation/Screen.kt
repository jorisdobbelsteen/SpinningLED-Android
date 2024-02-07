package nl.joris2k.spinningled.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen (
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    data object Connect : Screen("connect")

    data object Device : Screen("device/{hostname}",
        listOf(navArgument("hostname") { type = NavType.StringType })
    ) {
        fun createRoute(hostname: String) = "device/${hostname}"
    }
}
