package hcmus.bugscanner.ui.scan

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformScanScreen(
    isLoggedIn: Boolean,
    onAuthAction: () -> Unit,
    onDetectedBugClick: (String) -> Unit
)