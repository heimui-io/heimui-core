package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.port.HeimClock
import io.heimui.core.domain.port.HeimStorageDriver
import io.heimui.core.domain.port.InMemoryStorageDriver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Schema version of the cache envelope. Bumped whenever [CachedScreenEntry] changes shape. */
public const val HEIM_CACHE_SCHEMA_VERSION: Int = 2

@Serializable
public data class CachedScreenEntry(
    val screen: HeimScreenResponseDto,
    val etag: String? = null,
    val cachedAtMillis: Long = 0L,
    /** Signature that accompanied the payload, so a cache hit can be re-verified before render. */
    val signature: String? = null,
    /** Exact bytes the signature was computed over. Verification is meaningless without them. */
    val rawJson: String? = null,
    val schemaVersion: Int = HEIM_CACHE_SCHEMA_VERSION
) {
    val rawBytes: ByteArray? get() = rawJson?.encodeToByteArray()
}

public interface HeimCacheDataSource {
    public suspend fun getScreen(screenId: String): CachedScreenEntry?
    public suspend fun saveScreen(
        screenId: String,
        screen: HeimScreenResponseDto,
        etag: String? = null,
        signature: String? = null,
        rawBytes: ByteArray? = null
    )
    public suspend fun clear(screenId: String)
    public suspend fun clearAll()
}

public class DriverBackedHeimCacheDataSource(
    private val driver: HeimStorageDriver = InMemoryStorageDriver(),
    private val json: Json = HeimJson.instance,
    private val clock: HeimClock = HeimClock.System,
    /** Entries older than this are dropped. Null disables expiry. */
    private val ttlMillis: Long? = DEFAULT_TTL_MILLIS
) : HeimCacheDataSource {

    override suspend fun getScreen(screenId: String): CachedScreenEntry? {
        val raw = driver.get(screenId) ?: return null
        val entry = try {
            json.decodeFromString(CachedScreenEntry.serializer(), raw)
        } catch (_: Throwable) {
            null
        }
        // A cache written by an older SDK may decode into a different shape; drop it rather than
        // rendering a half-populated entry.
        if (entry == null || entry.schemaVersion != HEIM_CACHE_SCHEMA_VERSION) {
            driver.delete(screenId)
            return null
        }
        if (entry.isExpired(clock.nowMillis(), ttlMillis)) {
            driver.delete(screenId)
            return null
        }
        return entry
    }

    override suspend fun saveScreen(
        screenId: String,
        screen: HeimScreenResponseDto,
        etag: String?,
        signature: String?,
        rawBytes: ByteArray?
    ) {
        val entry = CachedScreenEntry(
            screen = screen,
            etag = etag,
            cachedAtMillis = clock.nowMillis(),
            signature = signature,
            rawJson = rawBytes?.decodeToString()
        )
        driver.put(screenId, json.encodeToString(CachedScreenEntry.serializer(), entry))
    }

    override suspend fun clear(screenId: String): Unit = driver.delete(screenId)

    override suspend fun clearAll(): Unit = driver.clear()

    public companion object {
        /** 7 days: long enough to be useful offline, short enough not to serve last month's UI. */
        public const val DEFAULT_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

internal fun CachedScreenEntry.isExpired(nowMillis: Long, ttlMillis: Long?): Boolean {
    if (ttlMillis == null || cachedAtMillis <= 0L) return false
    // A clock that moved backwards (timezone edit, NTP correction) must not resurrect an entry
    // forever, nor invalidate everything; treat it as fresh and let the ETag decide.
    val age = nowMillis - cachedAtMillis
    return age > ttlMillis
}

public class InMemoryHeimCacheDataSource(
    private val clock: HeimClock = HeimClock.System
) : HeimCacheDataSource {
    private val cache = mutableMapOf<String, CachedScreenEntry>()
    private val mutex = Mutex()

    override suspend fun getScreen(screenId: String): CachedScreenEntry? =
        mutex.withLock { cache[screenId] }

    override suspend fun saveScreen(
        screenId: String,
        screen: HeimScreenResponseDto,
        etag: String?,
        signature: String?,
        rawBytes: ByteArray?
    ) {
        mutex.withLock {
            cache[screenId] = CachedScreenEntry(
                screen = screen,
                etag = etag,
                cachedAtMillis = clock.nowMillis(),
                signature = signature,
                rawJson = rawBytes?.decodeToString()
            )
        }
    }

    override suspend fun clear(screenId: String) { mutex.withLock { cache.remove(screenId) } }

    override suspend fun clearAll(): Unit = mutex.withLock { cache.clear() }
}
