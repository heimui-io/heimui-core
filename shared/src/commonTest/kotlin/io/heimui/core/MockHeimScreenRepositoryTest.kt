package io.heimui.core

import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.data.repository.MockHeimScreenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockHeimScreenRepositoryTest {

    @Test
    fun `test mock repository delivers screen successfully`() = runTest {
        val json = """
            {
                "id": "mock_home",
                "version": "1.0.0",
                "title": "Mock Home",
                "root": {
                    "type": "container",
                    "id": "root_1"
                }
            }
        """.trimIndent()

        val repository = MockHeimScreenRepository(
            jsonProvider = { screenId ->
                if (screenId == "home") json else null
            }
        )

        val result = repository.getScreen("home", emptyMap()).first()
        assertTrue(result is HeimScreenResult.Success)
        assertEquals("mock_home", result.screen.id)
        assertEquals("Mock Home", result.screen.title)

        val missingResult = repository.getScreen("missing", emptyMap()).first()
        assertTrue(missingResult is HeimScreenResult.Error)
    }
}
