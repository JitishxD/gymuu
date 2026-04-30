package me.jitish.gymuu.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Modifier.clearFocusOnKeyboardDismiss(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible
    var isFocused by remember { mutableStateOf(false) }
    var wasImeVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = isFocused) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        wasImeVisible = false
    }

    LaunchedEffect(isFocused, isImeVisible) {
        when {
            isImeVisible -> wasImeVisible = true
            isFocused && wasImeVisible -> {
                focusManager.clearFocus(force = true)
                wasImeVisible = false
            }
            !isFocused -> wasImeVisible = false
        }
    }

    return onFocusChanged { focusState ->
        isFocused = focusState.isFocused
        if (!focusState.isFocused) {
            wasImeVisible = false
        }
    }
}
