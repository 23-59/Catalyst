package com.goldenminute.catalyst.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.DialogProperties
import com.goldenminute.catalyst.LocalizedContent
import java.util.Locale

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language
        ?: Locale.getDefault().language
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        LocalizedContent(localeCode = currentLocale) {
            content()
        }
    }
}

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = androidx.compose.material3.AlertDialogDefaults.shape,
    containerColor: Color = androidx.compose.material3.AlertDialogDefaults.containerColor,
    iconContentColor: Color = androidx.compose.material3.AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = androidx.compose.material3.AlertDialogDefaults.titleContentColor,
    textContentColor: Color = androidx.compose.material3.AlertDialogDefaults.textContentColor,
    tonalElevation: androidx.compose.ui.unit.Dp = androidx.compose.material3.AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language
        ?: Locale.getDefault().language
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { LocalizedContent(currentLocale) { confirmButton() } },
        modifier = modifier,
        dismissButton = dismissButton?.let { { LocalizedContent(currentLocale) { it() } } },
        icon = icon?.let { { LocalizedContent(currentLocale) { it() } } },
        title = title?.let { { LocalizedContent(currentLocale) { it() } } },
        text = text?.let { { LocalizedContent(currentLocale) { it() } } },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
