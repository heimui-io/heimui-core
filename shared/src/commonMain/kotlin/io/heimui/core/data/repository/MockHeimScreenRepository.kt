package io.heimui.core.data.repository

import io.heimui.core.data.mapper.toDomain
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import io.heimui.core.domain.model.HeimValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock repository for local testing, previewing, and debugging without a backend server.
 *
 * Lives in `data`, not `domain`: it parses wire JSON and maps DTOs, so keeping it in the domain
 * made the domain layer depend on the transport format it is supposed to be independent of.
 */
public class MockHeimScreenRepository(
    private val jsonProvider: (screenId: String) -> String?,
    private val simulatedDelayMillis: Long = 0L
) : HeimScreenRepository {

    override fun getScreen(
        screenId: String,
        queryParams: Map<String, String>
    ): Flow<HeimScreenResult> = flow {
        if (simulatedDelayMillis > 0) {
            delay(simulatedDelayMillis)
        }

        val rawJson = jsonProvider(screenId)
        if (rawJson == null) {
            emit(HeimScreenResult.Error(message = "Mock screen not found: $screenId"))
            return@flow
        }

        val result: HeimScreenResult = try {
            val dto = io.heimui.core.data.serialization.HeimJson.decodeScreen(rawJson)
            HeimScreenResult.Success(screen = dto.toDomain(), isStale = false)
        } catch (e: Exception) {
            HeimScreenResult.Error(message = "Failed to parse mock JSON: ${e.message}", throwable = e)
        }
        emit(result)
    }

    override suspend fun submitForm(
        endpoint: String,
        method: String,
        payload: Map<String, HeimValue>?
    ): HeimSubmitResult {
        if (simulatedDelayMillis > 0) {
            delay(simulatedDelayMillis)
        }
        return HeimSubmitResult.Success(
            responseScreen = null,
            message = "Mock submission to $endpoint successful"
        )
    }
}
