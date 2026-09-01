package io.heimui.core

import io.heimui.core.di.HeimConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HeimUITest {

    @BeforeTest
    fun setup() {
        HeimUI.reset()
    }

    @AfterTest
    fun tearDown() {
        HeimUI.reset()
    }

    @Test
    fun `test HeimUI initialization lifecycle`() {
        assertFalse(HeimUI.isInitialized)

        val config = HeimConfig(
            baseUrl = "https://api.heimui.io/sdui",
            authTokenProvider = { "mock_jwt_token_123" }
        )

        HeimUI.initialize(config)

        assertTrue(HeimUI.isInitialized)
        assertEquals("https://api.heimui.io/sdui", HeimUI.config.baseUrl)
        assertEquals("mock_jwt_token_123", HeimUI.config.authTokenProvider?.invoke())
        assertNotNull(HeimUI.repository)

        HeimUI.reset()
        assertFalse(HeimUI.isInitialized)
    }
}
