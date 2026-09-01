package io.heimui.core.domain.repository

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.mapper.toDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Mock repository for local testing, previewing, and debugging without a backend server.
 */
class MockHeimScreenRepository(
    private val jsonProvider: (screenId: String) -> String?,
    private val simulatedDelayMillis: Long = 0L,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
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
            val dto = json.decodeFromString<HeimScreenResponseDto>(rawJson)
            HeimScreenResult.Success(screen = dto.toDomain(), isStale = false)
        } catch (e: Exception) {
            HeimScreenResult.Error(message = "Failed to parse mock JSON: ${e.message}", throwable = e)
        }
        emit(result)
    }

    override suspend fun submitForm(
        endpoint: String,
        method: String,
        payload: Map<String, Any?>?
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
