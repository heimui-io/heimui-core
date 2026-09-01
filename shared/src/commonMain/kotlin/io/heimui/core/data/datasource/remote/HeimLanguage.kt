package io.heimui.core.data.datasource.remote

/**
 * The reader's language, as an IETF tag such as `es-CO`.
 *
 * Sent as `Accept-Language` on every screen request. Without it a backend has no idea who it is
 * answering, so it cannot localise a payload even when it wants to — and localising on the server
 * is the right place for it, since the alternative is shipping every translation to every device.
 */
internal expect fun heimDeviceLanguageTag(): String?
