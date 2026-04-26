package me.jitish.gymuu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import me.jitish.gymuu.ui.GymuuApp
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.theme.GymuuTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GymViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    companion object {
        private const val APP_SCALE = 0.9f
    }
}

