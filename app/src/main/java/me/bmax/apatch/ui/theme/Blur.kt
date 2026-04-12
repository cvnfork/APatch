package me.bmax.apatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun Modifier.miuixBlur(
    backdrop: Backdrop,
    shape: Shape = RectangleShape,
    radius: Float = 25f,
    alpha: Float = 0.8f
): Modifier {
    val blendColor = MiuixTheme.colorScheme.surface.copy(alpha = alpha)

    return this.textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = radius,
        colors = BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = blendColor)
            )
        )
    )
}