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
