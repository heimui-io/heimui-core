package io.heimui.core

import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.NavigateAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.component.*
import kotlinx.serialization.encodeToString
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
    fun testSerializeAndDeserializeScreenPayload() {
        val screen = HeimScreenResponse(
            id = "home_screen",
            version = "1.0.0",
            title = "Home",
            applySafeInsets = true,
            root = ContainerComponent(
                id = "root_container",
                direction = Direction.VERTICAL,
                padding = 16,
                spacing = 12,
                children = listOf(
                    TextComponent(
                        id = "title_text",
                        text = "Welcome to HeimUI",
                        style = "titleLarge",
                        a11y = HeimAccessibility(
                            contentDescription = "Welcome header",
                            role = AccessibilityRole.HEADER,
                            isHeading = true
                        )
                    ),
                    ImageComponent(
                        id = "hero_banner",
                        url = "https://example.com/banner.png",
                        aspectRatio = 1.77f,
                        cornerRadius = 8
                    ),
                    CardComponent(
                        id = "info_card",
                        elevation = 4,
                        cornerRadius = 12,
                        child = TextComponent(
                            id = "card_content",
                            text = "Card inside HeimUI"
                        ),
                        actions = listOf(
                            NavigateAction(screenId = "details_screen")
                        )
                    ),
                    ButtonComponent(
                        id = "submit_btn",
                        title = "Continue",
                        variant = ButtonVariant.FILLED,
                        actions = listOf(
                            SubmitFormAction(endpoint = "/api/v1/submit")
                        )
                    ),
                    SwitchComponent(
                        id = "notifications_switch",
                        stateKey = "notifications_enabled",
                        label = "Enable Notifications",
                        initialChecked = true
                    )
                )
            )
        )

        val encoded = json.encodeToString(screen)
        assertTrue(encoded.contains("\"type\":\"container\""))
        assertTrue(encoded.contains("\"type\":\"text\""))
        assertTrue(encoded.contains("\"type\":\"image\""))
        assertTrue(encoded.contains("\"type\":\"card\""))
        assertTrue(encoded.contains("\"type\":\"button\""))
        assertTrue(encoded.contains("\"type\":\"switch\""))

        val decoded = json.decodeFromString<HeimScreenResponse>(encoded)
        assertEquals("home_screen", decoded.id)
        assertIs<ContainerComponent>(decoded.root)
        val container = decoded.root
        assertEquals(5, container.children.size)
        assertIs<TextComponent>(container.children[0])
        assertIs<ImageComponent>(container.children[1])
        assertIs<CardComponent>(container.children[2])
        assertIs<ButtonComponent>(container.children[3])
        assertIs<SwitchComponent>(container.children[4])
    }
}
