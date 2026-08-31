package io.heimui.core.presentation.launcher

import android.content.Intent
import android.net.Uri

class AndroidUrlLauncher : HeimUrlLauncher {
    override fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }
}

actual fun createDefaultUrlLauncher(): HeimUrlLauncher = AndroidUrlLauncher()
