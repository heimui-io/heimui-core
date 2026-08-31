package io.heimui.core.presentation.launcher

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosUrlLauncher : HeimUrlLauncher {
    override fun openUrl(url: String): Boolean {
        return try {
            val nsUrl = NSURL.URLWithString(url) ?: return false
            UIApplication.sharedApplication.openURL(nsUrl)
        } catch (_: Throwable) {
            false
        }
    }
}

actual fun createDefaultUrlLauncher(): HeimUrlLauncher = IosUrlLauncher()
