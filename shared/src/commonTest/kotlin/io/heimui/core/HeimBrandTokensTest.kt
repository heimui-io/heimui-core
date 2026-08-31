package io.heimui.core

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.heimui.core.presentation.designsystem.HeimBrandTokens
import io.heimui.core.presentation.designsystem.HeimTokenResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class HeimBrandTokensTest {

    @Test
    fun `test custom brand tokens override default colors`() {
        val customBrandColor = Color(0xFFFF5722)
        val brandTokens = HeimBrandTokens.build {
            color("bankGold", customBrandColor)
        }

        val colorScheme = darkColorScheme()

        // 1. Custom brand token resolution
        val resolvedBrandColor = HeimTokenResolver.resolveColor(
            tokenOrHex = "bankGold",
            colorScheme = colorScheme,
            brandTokens = brandTokens
        )
        assertEquals(customBrandColor, resolvedBrandColor)

        // 2. Standard Material 3 token fallback
        val resolvedPrimary = HeimTokenResolver.resolveColor(
            tokenOrHex = "primary",
            colorScheme = colorScheme,
            brandTokens = brandTokens
        )
        assertEquals(colorScheme.primary, resolvedPrimary)
    }
}
