package io.heimui.core.data.datasource.local

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.port.HeimClock
import io.heimui.core.domain.port.HeimStorageDriver
import io.heimui.core.domain.port.InMemoryStorageDriver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
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
    private val ttlMillis: Long? = DEFAULT_TTL_MILLIS,
    /**
     * How many screens to keep. The oldest is dropped when the limit is reached.
     *
     * A bound is necessary, not decorative. Entries are keyed by the URL fetched, so a catalogue
     * browsed through a hundred products leaves a hundred entries — on the user's device, in
     * storage they did not agree to spend. Time alone does not bound that: a TTL only removes what
     * nobody came back for, and says nothing about how much accumulates in the meantime.
     */
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) : HeimCacheDataSource {

    // Writes are read-modify-write across two keys, so they have to be serialised even though the
    // driver itself is thread-safe.
    private val mutex = Mutex()

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
            evict(screenId)
            return null
        }
        if (entry.isExpired(clock.nowMillis(), ttlMillis)) {
            evict(screenId)
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
        mutex.withLock {
            driver.put(screenId, json.encodeToString(CachedScreenEntry.serializer(), entry))
            touchAndTrim(screenId)
        }
    }

    override suspend fun clear(screenId: String): Unit = mutex.withLock { evict(screenId) }

    override suspend fun clearAll(): Unit = mutex.withLock {
        driver.clear()
    }

    private suspend fun evict(key: String) {
        driver.delete(key)
        val index = readIndex()
        if (index.remove(key)) writeIndex(index)
    }

    /**
     * Records the key as most recently written and drops the oldest beyond [maxEntries].
     *
     * The index is kept as its own document because [HeimStorageDriver] is four methods and none
     * of them enumerates keys — asking implementors for a listing API would make the simplest
     * possible driver harder to write, for a need only this class has.
     */
    private suspend fun touchAndTrim(key: String) {
        val index = readIndex()
        index.remove(key)
        index.add(key)
        while (index.size > maxEntries) {
            driver.delete(index.removeAt(0))
        }
        writeIndex(index)
    }

    private suspend fun readIndex(): MutableList<String> {
        val raw = driver.get(INDEX_KEY) ?: return mutableListOf()
        return try {
            json.decodeFromString(ListSerializer(String.serializer()), raw).toMutableList()
        } catch (_: Throwable) {
            // A corrupt index costs eviction ordering, not correctness: entries still expire by
            // TTL. Starting over beats refusing to cache.
            mutableListOf()
        }
    }

    private suspend fun writeIndex(index: List<String>) {
        // An empty index is litter, not state. Leaving the document behind means clearing the last
        // screen still leaves something in the user's storage.
        if (index.isEmpty()) {
            driver.delete(INDEX_KEY)
            return
        }
        driver.put(INDEX_KEY, json.encodeToString(ListSerializer(String.serializer()), index))
    }

    public companion object {
        /** 7 days: long enough to be useful offline, short enough not to serve last month's UI. */
        public const val DEFAULT_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

        /**
         * Enough for a deep session without the cache becoming a place things accumulate.
         * A payload is kilobytes, so this is single-digit megabytes at worst.
         */
        public const val DEFAULT_MAX_ENTRIES: Int = 60

        private const val INDEX_KEY = "heim_cache_index"
    }
}

internal fun CachedScreenEntry.isExpired(nowMillis: Long, ttlMillis: Long?): Boolean {
    if (ttlMillis == null || cachedAtMillis <= 0L) return false
    // A clock that moved backwards (timezone edit, NTP correction) must not resurrect an entry
    // forever, nor invalidate everything; treat it as fresh and let the ETag decide.
    val age = nowMillis - cachedAtMillis
    return age > ttlMillis
}

/**
 * A cache that never stores anything: every screen open goes to the network.
 *
 * Pass it as `HeimConfig(customCacheDataSource = NoHeimCacheDataSource())` when content must
 * always be fresh — a live price, a regulatory disclosure, a screen whose staleness would be
 * misleading.
 *
 * Understand what you give up. Without a cache there is no stale-while-revalidate, so the user
 * waits on the network before seeing anything; there is no offline behaviour at all; and ETag
 * revalidation stops working, because a 304 has no cached copy to serve — so every open
 * re-downloads the full payload instead of exchanging a few hundred bytes.
 *
 * For most screens the default cache with ETag revalidation is both fresher-feeling and cheaper:
 * the client still asks the server on every open, it just avoids re-downloading an unchanged
 * answer. Reach for this only when you truly cannot show a previous version for an instant.
 */
public class NoHeimCacheDataSource : HeimCacheDataSource {
    override suspend fun getScreen(screenId: String): CachedScreenEntry? = null
    override suspend fun saveScreen(
        screenId: String,
        screen: HeimScreenResponseDto,
        etag: String?,
        signature: String?,
        rawBytes: ByteArray?,
    ): Unit = Unit
    override suspend fun clear(screenId: String): Unit = Unit
    override suspend fun clearAll(): Unit = Unit
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
