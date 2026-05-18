package hcmus.bugscanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import hcmus.bugscanner.ui.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}