package io.heimui.core

import io.heimui.core.data.dto.*
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.domain.model.action.DismissModalAction
import io.heimui.core.domain.model.action.ShowBottomSheetAction
import io.heimui.core.domain.model.action.ShowDialogAction
import io.heimui.core.domain.model.component.TextComponent
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeimModalActionSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `test serialize and deserialize show_bottom_sheet action`() {
        val rawJson = """
            {
                "type": "show_bottom_sheet",
                "title": "Select Option",
                "is_dismissible": true,
                "content": {
                    "type": "text",
                    "id": "sheet_text",
                    "text": "Bottom sheet content"
                }
            }
        """.trimIndent()

        val dto = json.decodeFromString<HeimActionDto>(rawJson)
        assertTrue(dto is ShowBottomSheetActionDto)
        assertEquals("Select Option", dto.title)
        assertTrue(dto.isDismissible)

        val domain = dto.toDomain()
        assertTrue(domain is ShowBottomSheetAction)
        assertEquals("Select Option", domain.title)
        val content = domain.content
        assertTrue(content is TextComponent)
        assertEquals("Bottom sheet content", content.text)
    }

    @Test
    fun `test serialize and deserialize show_dialog action`() {
        val rawJson = """
            {
                "type": "show_dialog",
                "title": "Confirm Action",
                "message": "Are you sure you want to proceed?",
                "confirm_text": "Yes, continue",
                "confirm_actions": [
                    {
                        "type": "dismiss_modal"
                    }
                ],
                "dismiss_text": "Cancel"
            }
        """.trimIndent()

        val dto = json.decodeFromString<HeimActionDto>(rawJson)
        assertTrue(dto is ShowDialogActionDto)
        assertEquals("Confirm Action", dto.title)
        assertEquals("Are you sure you want to proceed?", dto.message)
        assertEquals("Yes, continue", dto.confirmText)
        assertEquals("Cancel", dto.dismissText)

        val domain = dto.toDomain()
        assertTrue(domain is ShowDialogAction)
        assertEquals("Confirm Action", domain.title)
        assertEquals(1, domain.confirmActions.size)
        assertTrue(domain.confirmActions.first() is DismissModalAction)
    }
}
