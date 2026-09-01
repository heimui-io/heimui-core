package io.heimui.core

import io.heimui.core.data.datasource.local.DefaultHeimEmergencyBundleProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimEmergencyBundleTest {

    @Test
    fun `test emergency bundle fallback provider`() = runTest {
        val fallbackJson = """
            {
                "id": "emergency_offline_screen",
                "version": "1.0.0",
                "title": "Offline Mode",
                "root": {
                    "type": "container",
                    "id": "root_offline"
                }
            }
        """.trimIndent()

        val provider = DefaultHeimEmergencyBundleProvider(
            bundles = mapOf("emergency_offline_screen" to fallbackJson)
        )

        val screen = provider.getEmergencyScreen("emergency_offline_screen")
        assertNotNull(screen)
        assertEquals("emergency_offline_screen", screen.id)
        assertEquals("Offline Mode", screen.title)

        // Non-existent screen
        assertNull(provider.getEmergencyScreen("unknown_screen"))
    }
}
