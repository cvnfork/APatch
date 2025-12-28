package me.bmax.apatch.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun IconTextButton(
    iconRes: ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = true,
        backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = iconRes,
            contentDescription = null
        )
    }
}

@Composable
fun ModuleStateIndicator(
    icon: ImageVector, color: Color = MiuixTheme.colorScheme.outline
) {
    Image(
        modifier = Modifier.requiredSize(150.dp),
        imageVector = icon,
        contentDescription = null,
        alpha = 0.1f,
        colorFilter = ColorFilter.tint(color)
    )
}