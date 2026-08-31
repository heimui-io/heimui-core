package io.heimui.core

import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.component.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HeimComponentSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    @Test
    fun testParseJsonToDtoAndMapToPureDomain() {
        val rawJson = """
        {
            "id": "home_screen",
            "version": "1.0.0",
            "title": "Home",
            "apply_safe_insets": true,
            "root": {
                "type": "container",
                "id": "root_container",
                "direction": "VERTICAL",
                "padding": 16,
                "spacing": 12,
                "children": [
                    {
                        "type": "text",
                        "id": "title_text",
                        "text": "Welcome to HeimUI",
                        "style": "titleLarge",
                        "a11y": {
                            "content_description": "Welcome header",
                            "role": "HEADER",
                            "is_heading": true
                        }
                    },
                    {
                        "type": "image",
                        "id": "hero_banner",
                        "url": "https://example.com/banner.png",
                        "aspect_ratio": 1.77,
                        "corner_radius": 8
                    },
                    {
                        "type": "card",
                        "id": "info_card",
                        "elevation": 4,
                        "corner_radius": 12,
                        "actions": [
                            {
                                "type": "navigate",
                                "screen_id": "details_screen"
                            }
                        ],
                        "child": {
                            "type": "text",
                            "id": "card_content",
                            "text": "Card inside HeimUI"
                        }
                    },
                    {
                        "type": "button",
                        "id": "submit_btn",
                        "title": "Continue",
                        "variant": "FILLED",
                        "actions": [
                            {
                                "type": "submit_form",
                                "endpoint": "/api/v1/submit"
                            }
                        ]
                    },
                    {
                        "type": "switch",
                        "id": "notifications_switch",
                        "state_key": "notifications_enabled",
                        "label": "Enable Notifications",
                        "initial_checked": true
                    },
                    {
                        "type": "custom",
                        "id": "chart_1",
                        "name": "stock_chart",
                        "data": {
                            "ticker": "AAPL",
                            "volume": 15000
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        // 1. Parse into Data Layer DTO
        val dto = json.decodeFromString<HeimScreenResponseDto>(rawJson)
        assertEquals("home_screen", dto.id)

        // 2. Map into Pure Domain Entity
        val domainScreen = dto.toDomain()
        assertEquals("home_screen", domainScreen.id)
        assertIs<ContainerComponent>(domainScreen.root)

        val container = domainScreen.root
        assertEquals(6, container.children.size)
        assertIs<TextComponent>(container.children[0])
        assertEquals("Welcome header", container.children[0].a11y?.contentDescription)
        assertEquals(AccessibilityRole.HEADER, container.children[0].a11y?.role)
        assertTrue(container.children[0].a11y?.isHeading == true)

        assertIs<ImageComponent>(container.children[1])
        assertIs<CardComponent>(container.children[2])
        assertIs<ButtonComponent>(container.children[3])
        assertIs<SwitchComponent>(container.children[4])

        // 3. Custom Component with pure Map data
        val custom = container.children[5]
        assertIs<CustomComponent>(custom)
        assertEquals("stock_chart", custom.name)
        assertEquals("AAPL", custom.data["ticker"])
        assertEquals(15000L, custom.data["volume"])
    }
}
