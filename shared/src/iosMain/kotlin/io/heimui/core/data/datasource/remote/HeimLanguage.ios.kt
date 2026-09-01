package io.heimui.core.data.datasource.remote

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

internal actual fun heimDeviceLanguageTag(): String? =
    // `preferredLanguages` is ordered by the user's own preference and already returns IETF tags.
    (NSLocale.preferredLanguages.firstOrNull() as? String)?.takeIf { it.isNotBlank() }
