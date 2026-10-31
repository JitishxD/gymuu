package me.jitish.gymuu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import me.jitish.gymuu.ui.AppViewModel
import me.jitish.gymuu.ui.exercise.RestTimerNotifier
import me.jitish.gymuu.ui.navigation.GymuuApp
import me.jitish.gymuu.ui.theme.GymuuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The UI is always dark, so force light system-bar icons regardless of system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        handleRestCompleteIntent(intent)
        if (savedInstanceState == null) requestNotificationPermissionIfNeeded()

        setContent {
            GymuuTheme {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        base.density * APP_SCALE,
                        base.fontScale * APP_SCALE
                    )
                ) {
                    GymuuApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRestCompleteIntent(intent)
    }

    private fun handleRestCompleteIntent(intent: Intent?) {
        if (intent?.action != RestTimerNotifier.ACTION_OPEN_REST_COMPLETE) return
        val id = intent.getStringExtra(RestTimerNotifier.EXTRA_ROUTINE_EXERCISE_ID)
            ?.takeIf { it.isNotBlank() } ?: return
        RestTimerNotifier(this).cancelRestCompleteAlert(id)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val APP_SCALE = 0.9f
    }
}
