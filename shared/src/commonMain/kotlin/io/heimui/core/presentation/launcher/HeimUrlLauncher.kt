package io.heimui.core.presentation.launcher

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Interface for launching external URLs in system browsers across platforms.
 */
interface HeimUrlLauncher {
    fun openUrl(url: String): Boolean
}

expect fun createDefaultUrlLauncher(): HeimUrlLauncher

val LocalHeimUrlLauncher = staticCompositionLocalOf<HeimUrlLauncher> {
    createDefaultUrlLauncher()
}
