package io.heimui.core.data.datasource.remote

import java.util.Locale

internal actual fun heimDeviceLanguageTag(): String? =
    Locale.getDefault().toLanguageTag().takeIf { it.isNotBlank() && it != "und" }
