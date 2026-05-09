package me.jitish.gymuu

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.exercise.RestTimerNotifier
import me.jitish.gymuu.ui.navigation.GymuuApp
import me.jitish.gymuu.ui.theme.GymuuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GymViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestExactAlarmPermissionIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRestCompleteNotificationIntent(intent)
        enableEdgeToEdge()
        if (!requestNotificationPermissionIfNeeded()) {
            requestExactAlarmPermissionIfNeeded()
        }
        setContent {
            GymuuTheme {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density * APP_SCALE,
                        fontScale = baseDensity.fontScale * APP_SCALE
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
        handleRestCompleteNotificationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegroundChanged(true)
    }

    override fun onStop() {
        viewModel.onAppForegroundChanged(false)
        super.onStop()
    }

    private fun handleRestCompleteNotificationIntent(intent: Intent?) {
        if (intent?.action != RestTimerNotifier.ACTION_OPEN_REST_COMPLETE) return

        val routineExerciseId = intent.getStringExtra(RestTimerNotifier.EXTRA_ROUTINE_EXERCISE_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return

        RestTimerNotifier(this).cancelRestCompleteAlert(routineExerciseId)
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return true
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return

        val prefs = getSharedPreferences(PERMISSION_PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(EXACT_ALARM_PERMISSION_REQUESTED_KEY, false)) return

        prefs.edit {
            putBoolean(EXACT_ALARM_PERMISSION_REQUESTED_KEY, true)
        }

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
        }
        runCatching { startActivity(intent) }
    }

    companion object {
        private const val APP_SCALE = 0.9f
        private const val PERMISSION_PREFS_NAME = "gymuu_permission_requests"
        private const val EXACT_ALARM_PERMISSION_REQUESTED_KEY = "exact_alarm_permission_requested"
    }
}

