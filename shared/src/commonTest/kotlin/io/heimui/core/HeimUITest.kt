package io.heimui.core

import io.heimui.core.data.datasource.remote.HeimAuthContext
import io.heimui.core.di.HeimConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
            // Screens come from a CDN, writes go to our own API: the provider sees which is which
            // so a credential is never handed to a host that has no business holding it.
            authTokenProvider = { context ->
                when (context) {
                    is HeimAuthContext.ScreenFetch -> null
                    is HeimAuthContext.FormSubmit -> "mock_jwt_token_123"
                }
            }
        )

        HeimUI.initialize(config)

        assertTrue(HeimUI.isInitialized)
        assertEquals("https://api.heimui.io/sdui", HeimUI.config.baseUrl)
        val provider = assertNotNull(HeimUI.config.authTokenProvider)
        assertNull(provider.authHeaderFor(HeimAuthContext.ScreenFetch("https://cdn.example/a.json")))
        assertEquals(
            "mock_jwt_token_123",
            provider.authHeaderFor(HeimAuthContext.FormSubmit("https://api.heimui.io/kyc"))
        )
        assertNotNull(HeimUI.repository)

        HeimUI.reset()
        assertFalse(HeimUI.isInitialized)
    }
}
