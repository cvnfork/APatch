package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ArrowItem(
    title: String,
    summary: String,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    SuperArrow(
        title = title,
        summary = summary,
        onClick = onClick,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(end = 6.dp),
                tint = colorScheme.onBackground
            )
        }
    )
}

@Composable
fun SwitchItem(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    SuperSwitch(
        title = title,
        summary = summary,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        startAction = {
            Icon(
                imageVector = icon,
                modifier = Modifier.padding(end = 6.dp),
                contentDescription = contentDescription,
                tint = colorScheme.onBackground
            )
        }
    )
}