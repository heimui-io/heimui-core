package io.heimui.core.domain.repository

import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.HeimValue
import kotlinx.coroutines.flow.Flow

public sealed interface HeimScreenResult {
    /** Fresh, verified content. [isStale] is true when it came from cache pending revalidation. */
    public data class Success(val screen: HeimScreenResponse, val isStale: Boolean = false) : HeimScreenResult

    /**
     * Revalidation failed but usable content is still available.
     *
     * Distinct from [Error] on purpose: emitting a terminal error after a stale success would
     * make the UI replace readable content with an error screen, which is strictly worse for an
     * offline user than showing them the cached screen.
     */
    public data class Stale(
        val screen: HeimScreenResponse,
        val reason: String,
        val throwable: Throwable? = null
    ) : HeimScreenResult

    /**
     * A screen the server sent with a 4xx, to be shown instead of the one that was asked for.
     *
     * Content, not a failure: the reader sees what the server meant them to see, and the SDK's own
     * error view never appears. What separates it from [Success] is that it is **never cached and
     * never replaces a cached copy** -- it describes the state of the world at this moment, not the
     * contents of that screen, and a reinstated account that kept reading "suspended" from a cache
     * would be the SDK's fault rather than the server's.
     *
     * [statusCode] is carried through so the host can act on it. A 401 usually means the session
     * is gone and the app should say so in its own navigation, which it cannot decide by reading
     * the screen.
     */
    public data class Refused(
        val screen: HeimScreenResponse,
        val statusCode: Int
    ) : HeimScreenResult

    /** No content is available at all. */
    public data class Error(val message: String, val throwable: Throwable? = null) : HeimScreenResult
}

public sealed interface HeimSubmitResult {
    public data class Success(val responseScreen: HeimScreenResponse? = null, val message: String? = null) : HeimSubmitResult
    public data class Error(val message: String, val throwable: Throwable? = null) : HeimSubmitResult

    /** The submission was refused locally by the SDK security policy; it never left the device. */
    public data class Blocked(val message: String) : HeimSubmitResult
}

public interface HeimScreenRepository {
    public fun getScreen(screenId: String, queryParams: Map<String, String> = emptyMap()): Flow<HeimScreenResult>
    public suspend fun submitForm(endpoint: String, method: String = "POST", payload: Map<String, HeimValue>? = null): HeimSubmitResult
}
