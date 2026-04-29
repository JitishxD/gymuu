package me.jitish.gymuu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.jitish.gymuu.ui.theme.GymBorder
import me.jitish.gymuu.ui.theme.GymCard
import me.jitish.gymuu.ui.theme.GymDanger
import me.jitish.gymuu.ui.theme.GymMuted

@Composable
internal fun GymInput(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    trailing: @Composable (() -> Unit)? = null,
    helper: String? = null,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(150)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6E6E6E)) },
            trailingIcon = trailing,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF151515),
                unfocusedContainerColor = Color(0xFF151515),
                focusedBorderColor = GymBorder,
                unfocusedBorderColor = GymBorder,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        helper?.let { Text(it, color = GymMuted, fontSize = 13.sp) }
    }
}

@Composable
internal fun GymInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailing: @Composable (() -> Unit)? = null,
    helper: String? = null,
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(150)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = GymMuted, fontSize = 14.sp, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF6E6E6E)) },
            trailingIcon = trailing,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF151515),
                unfocusedContainerColor = Color(0xFF151515),
                focusedBorderColor = GymBorder,
                unfocusedBorderColor = GymBorder,
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        helper?.let { Text(it, color = GymMuted, fontSize = 13.sp) }
    }
}

@Composable
internal fun NameDialog(title: String, initialValue: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by rememberSaveable(initialValue, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(textFieldValueAtEnd(initialValue))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = { Text(title, color = Color.White) },
        text = { GymInput(label = label, value = value, onValueChange = { value = it }, placeholder = "e.g. Push Day", autoFocus = true) },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }) { Text("CONFIRM", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = GymMuted) } }
    )
}

@Composable
internal fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCard,
        title = { Text(title, color = Color.White) },
        text = { Text(text, color = GymMuted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("DELETE", color = GymDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White) } }
    )
}

@Composable
internal fun InlineEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 110.dp,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    textColor: Color = Color.White,
    placeholderColor: Color = GymMuted
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        modifier = modifier.width(width),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box {
                if (value.isBlank()) Text(placeholder, color = placeholderColor, fontSize = 16.sp)
                inner()
            }
        }
    )
}

internal fun textFieldValueAtEnd(text: String): TextFieldValue {
    return TextFieldValue(
        text = text,
        selection = TextRange(text.length)
    )
}
