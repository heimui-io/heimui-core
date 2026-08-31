package io.heimui.core.presentation.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun HeimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme? = null,
    typography: Typography? = null,
    iconProvider: HeimIconProvider = DefaultHeimIconProvider,
    content: @Composable () -> Unit
) {
    val defaultColorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    val finalColorScheme = colorScheme ?: defaultColorScheme
    val finalTypography = typography ?: Typography()

    CompositionLocalProvider(
        LocalHeimIconProvider provides iconProvider
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = finalTypography,
            content = content
        )
    }
}
