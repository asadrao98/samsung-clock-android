package com.asadrao.clock.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Asks for notification permission once, on first launch.
 *
 * Needed from Android 13. Without it the ringing notification cannot be posted, and since that
 * notification is what carries the full-screen intent, a denied permission means an alarm that
 * makes a sound but shows nothing. The sound still plays — the foreground service does not depend
 * on the permission — so a refusal degrades the experience rather than breaking the alarm.
 *
 * Asked at launch rather than at the moment an alarm is created: the user should not discover at
 * 6am that they never granted it.
 */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val alreadyGranted = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Either way the app keeps working; nothing to do with the result here. */ }

    LaunchedEffect(alreadyGranted) {
        if (!alreadyGranted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
