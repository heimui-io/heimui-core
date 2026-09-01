package io.heimui.core.presentation.launcher

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.UriHandler

public interface HeimUrlLauncher {
    /** @return true if the URL was accepted and handed to the platform, false if it was refused. */
    public fun openUrl(url: String): Boolean
}

/**
 * Scheme policy for payload-supplied URLs.
 *
 * `OpenUrlAction.url` arrives from the server, so passing it unchecked to the platform is an
 * intent-redirection vector on Android (`intent://` can reach unexported components) and a local
 * file-disclosure vector on both platforms (`file://`, `content://`). Only an explicit allow-list
 * is safe here; a deny-list will always miss a scheme.
 */
public data class HeimUrlPolicy(
    val allowedSchemes: Set<String> = setOf("https"),
    /** Additional app-specific schemes, e.g. setOf("mybank") for first-party deep links. */
    val allowedCustomSchemes: Set<String> = emptySet()
) {
    public fun isAllowed(url: String): Boolean {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase().trim()
        if (scheme.isEmpty()) return false
        return scheme in allowedSchemes || scheme in allowedCustomSchemes
    }

    public companion object {
        public val default: HeimUrlPolicy = HeimUrlPolicy()
    }
}

/**
 * Default launcher, backed by Compose's own [UriHandler].
 *
 * Using the Compose handler rather than hand-rolled platform code is deliberate: it already
 * resolves to `startActivity` on Android and `UIApplication.openURL` on iOS, with the correct
 * Context/Application plumbing that a bare KMP class cannot obtain on its own.
 */
public class ComposeUriUrlLauncher(
    private val uriHandler: UriHandler,
    private val policy: HeimUrlPolicy = HeimUrlPolicy.default,
    private val onRefused: (url: String, reason: String) -> Unit = { _, _ -> }
) : HeimUrlLauncher {
    override fun openUrl(url: String): Boolean {
        if (!policy.isAllowed(url)) {
            onRefused(url, "Scheme is not permitted by HeimUrlPolicy")
            return false
        }
        return try {
            uriHandler.openUri(url)
            true
        } catch (_: Throwable) {
            onRefused(url, "Platform refused to open the URL")
            false
        }
    }
}

/** Explicit opt-out for hosts that route every navigation through their own `onAction`. */
public class NoOpUrlLauncher : HeimUrlLauncher {
    override fun openUrl(url: String): Boolean = false
}

/**
 * Defaults to a no-op so a host that never wired [io.heimui.core.presentation.designsystem.HeimTheme]
 * cannot silently open server-supplied URLs. HeimTheme installs a real [ComposeUriUrlLauncher].
 */
public val LocalHeimUrlLauncher: ProvidableCompositionLocal<HeimUrlLauncher> =
    staticCompositionLocalOf {
    NoOpUrlLauncher()
}
