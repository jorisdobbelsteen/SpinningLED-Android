package nl.joris2k.spinningled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppBar(title: String,
             navController: NavHostController,
             showBackButton: Boolean = true,
             content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> kotlin.Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
//                val showBackButton by remember(navController.currentBackStackEntryAsState()) {
//                    derivedStateOf {
//                        navController.previousBackStackEntry != null
//                    }
//                }
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(title)
                },
                navigationIcon = { if (showBackButton) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                } }
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}