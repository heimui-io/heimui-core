package io.heimui.core.domain.port

/**
 * Wall-clock source, injectable for tests.
 *
 * Cache expiry needs absolute time that survives a process restart, which a monotonic
 * TimeSource cannot provide. `cachedAtMillis` was hardcoded to 0L, so cached screens never
 * expired -- a stale screen stayed stale forever.
 */
public fun interface HeimClock {
    public fun nowMillis(): Long

    public companion object {
        public val System: HeimClock = HeimClock { currentTimeMillis() }
    }
}

internal expect fun currentTimeMillis(): Long
