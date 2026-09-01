package io.heimui.core

import io.heimui.core.data.dto.ButtonComponentDto
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.HeimComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.component.HeimArrangement
import io.heimui.core.domain.model.component.HeimPadding
import io.heimui.core.domain.model.component.HeimSize
import io.heimui.core.domain.model.component.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
        assertEquals(io.heimui.core.domain.model.HeimValue.Str("AAPL"), custom.data["ticker"])
        // Integers stay exact: routing them through Double corrupted anything above 2^53.
        assertEquals(io.heimui.core.domain.model.HeimValue.Int64(15000), custom.data["volume"])
    }

    @Test
    fun `button icon is optional and survives the round trip`() {
        val withIcon = HeimJson.instance.decodeFromString(
            HeimComponentDto.serializer(),
            """{"type":"button","id":"send","title":"Send","icon":"  Send_Money  "}"""
        )
        val domain = (withIcon as ButtonComponentDto).toDomain() as ButtonComponent
        assertEquals("  Send_Money  ", withIcon.icon)
        // Trimmed and normalised on the way in, so a payload typo does not silently miss the
        // provider's lookup table.
        assertEquals("Send_Money", domain.icon)

        // Absent and blank both mean "no icon" — a `"icon": ""` must not draw a missing glyph.
        val blank = HeimJson.instance.decodeFromString(
            HeimComponentDto.serializer(),
            """{"type":"button","id":"plain","title":"Plain","icon":""}"""
        )
        assertNull(((blank as ButtonComponentDto).toDomain() as ButtonComponent).icon)

        val absent = HeimJson.instance.decodeFromString(
            HeimComponentDto.serializer(),
            """{"type":"button","id":"plain","title":"Plain"}"""
        )
        assertNull(((absent as ButtonComponentDto).toDomain() as ButtonComponent).icon)
    }


    @Test
    fun `padding accepts a number and an object alike`() {
        fun paddingOf(json: String): HeimPadding =
            (HeimJson.instance.decodeFromString(HeimComponentDto.serializer(), json)
                as ContainerComponentDto).toDomain().let { (it as ContainerComponent).padding }

        // The number form is what every existing payload uses and must keep working unchanged.
        assertEquals(
            HeimPadding.all(16),
            paddingOf("""{"type":"container","id":"c","padding":16}""")
        )

        // Shorthands fill the sides they cover.
        assertEquals(
            HeimPadding(start = 16, top = 8, end = 16, bottom = 8),
            paddingOf("""{"type":"container","id":"c","padding":{"horizontal":16,"vertical":8}}""")
        )

        // An explicit side beats the shorthand that would otherwise set it. JSON does not
        // guarantee key order, so this must not depend on which appears first.
        assertEquals(
            HeimPadding(start = 0, top = 0, end = 16, bottom = 0),
            paddingOf("""{"type":"container","id":"c","padding":{"horizontal":16,"start":0}}""")
        )
        assertEquals(
            HeimPadding(start = 0, top = 0, end = 16, bottom = 0),
            paddingOf("""{"type":"container","id":"c","padding":{"start":0,"horizontal":16}}""")
        )

        // Absent means none, and a negative side is clamped rather than failing the screen.
        assertEquals(HeimPadding.None, paddingOf("""{"type":"container","id":"c"}"""))
        assertEquals(
            HeimPadding(start = 0, top = 4, end = 0, bottom = 0),
            paddingOf("""{"type":"container","id":"c","padding":{"start":-8,"top":4}}""")
        )

        // Garbage is cosmetic, not fatal: a malformed padding must not cost the whole screen.
        assertEquals(
            HeimPadding.None,
            paddingOf("""{"type":"container","id":"c","padding":"nonsense"}""")
        )
    }


    @Test
    fun `arrangement distributes along the container's own axis`() {
        fun containerOf(json: String) =
            (HeimJson.instance.decodeFromString(HeimComponentDto.serializer(), json)
                as ContainerComponentDto).toDomain() as ContainerComponent

        // Absent means the previous behaviour, so no existing payload shifts.
        assertEquals(
            HeimArrangement.PACKED,
            containerOf("""{"type":"container","id":"c"}""").arrangement
        )

        val row = containerOf(
            """{"type":"container","id":"c","direction":"HORIZONTAL",
                "arrangement":"SPACE_BETWEEN","alignment":"CENTER","spacing":8}"""
        )
        // The two are independent axes: arrangement spreads along the row, alignment centres
        // the children across its height. Conflating them is the usual confusion.
        assertEquals(HeimArrangement.SPACE_BETWEEN, row.arrangement)
        assertEquals(Alignment.CENTER, row.alignment)
        assertEquals(8, row.spacing)

        // An unknown value must not fail the screen: a newer server naming an arrangement this
        // client has never heard of should still render, packed.
        assertEquals(
            HeimArrangement.PACKED,
            containerOf("""{"type":"container","id":"c","arrangement":"DIAGONAL"}""").arrangement
        )
    }

    @Test
    fun `weight is carried by any child and ignored when meaningless`() {
        val row = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"r","direction":"HORIZONTAL","children":[
                 {"type":"card","id":"a","weight":2},
                 {"type":"card","id":"b","weight":1},
                 {"type":"card","id":"c"}]}}"""
        ).toDomain().root as ContainerComponent

        assertEquals(2f, row.children[0].weight)
        assertEquals(1f, row.children[1].weight)
        // Absent means "size yourself", not "weight 0" — a 0 would collapse the child to nothing.
        assertNull(row.children[2].weight)

        // A non-positive weight is not a layout instruction, it is a bug upstream. Compose throws
        // on it, so one bad number would cost the whole screen.
        val bad = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"container","id":"r","children":[
                 {"type":"card","id":"a","weight":0},{"type":"card","id":"b","weight":-3}]}}"""
        ).toDomain().root as ContainerComponent
        assertNull(bad.children[0].weight)
        assertNull(bad.children[1].weight)
    }

    @Test
    fun `frame constrains a component and drops what Compose would reject`() {
        fun frameOf(json: String): HeimSize =
            HeimJson.decodeScreen("""{"id":"s","root":$json}""").toDomain().root.frame

        assertEquals(
            HeimSize(minHeight = 120),
            frameOf("""{"type":"card","id":"c","frame":{"min_height":120}}""")
        )
        assertEquals(
            HeimSize(width = 48, height = 48),
            frameOf("""{"type":"box","id":"b","frame":{"width":48,"height":48}}""")
        )
        assertEquals(
            HeimSize(aspectRatio = 1.78f),
            frameOf("""{"type":"image","id":"i","frame":{"aspect_ratio":1.78}}""")
        )

        // Any component carries it, and a numeric string is the common backend bug.
        assertEquals(
            HeimSize(minHeight = 44),
            frameOf("""{"type":"button","id":"btn","frame":{"min_height":"44"}}""")
        )

        // Zero and negative are bugs upstream, not instructions. Compose throws on them, so one
        // bad number would cost the whole screen rather than one component's sizing.
        assertEquals(
            HeimSize.None,
            frameOf("""{"type":"card","id":"c","frame":{"height":0,"width":-10,"aspect_ratio":0}}""")
        )

        // Absent and malformed both mean "size yourself".
        assertEquals(HeimSize.None, frameOf("""{"type":"card","id":"c"}"""))
        assertEquals(HeimSize.None, frameOf("""{"type":"card","id":"c","frame":"tall"}"""))
    }

}
