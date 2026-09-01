package io.heimui.core.domain.port

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Low-level key-value storage port.
 *
 * Lives in the domain because both `data` (screen cache) and `presentation` (form drafts) depend
 * on it. Declaring it in `data` forced presentation to import the data layer directly, which
 * inverted the dependency rule -- presentation reached past the domain into infrastructure.
 *
 * Host apps plug in SQLite, Room, DataStore, Keychain, or an encrypted key-value store.
 */
public interface HeimStorageDriver {
    public suspend fun get(key: String): String?
    public suspend fun put(key: String, value: String)
    public suspend fun delete(key: String)
    public suspend fun clear()
}

/**
 * Mutex-guarded so concurrent screen loads cannot corrupt the map or throw
 * ConcurrentModificationException.
 */
public class InMemoryStorageDriver : HeimStorageDriver {
    private val memory = mutableMapOf<String, String>()
    private val mutex = Mutex()

    override suspend fun get(key: String): String? = mutex.withLock { memory[key] }
    override suspend fun put(key: String, value: String): Unit = mutex.withLock { memory[key] = value }
    override suspend fun delete(key: String) { mutex.withLock { memory.remove(key) } }
    override suspend fun clear(): Unit = mutex.withLock { memory.clear() }
}

