package io.heimui.core

import androidx.compose.ui.graphics.Color
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimTokenResolverTest {

    @Test
    fun testParseHexColor() {
        val red3 = HeimTokenResolver.parseHexColor("#F00")
        assertNotNull(red3)
        assertEquals(Color(255, 0, 0, 255), red3)

        val orange6 = HeimTokenResolver.parseHexColor("#FF5722")
        assertNotNull(orange6)
        assertEquals(Color(255, 87, 34, 255), orange6)

        val alpha8 = HeimTokenResolver.parseHexColor("#80FF5722")
        assertNotNull(alpha8)
        assertEquals(Color(255, 87, 34, 128), alpha8)

        assertNull(HeimTokenResolver.parseHexColor("invalid-color"))
        assertNull(HeimTokenResolver.parseHexColor("#12"))
    }
}
