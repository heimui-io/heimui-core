package io.heimui.core

import androidx.compose.ui.graphics.Color
import io.heimui.core.data.dto.HeimComponentDto
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.component.CardComponent
import io.heimui.core.domain.model.component.CheckboxComponent
import io.heimui.core.domain.model.component.ChipComponent
import io.heimui.core.domain.model.component.DatePickerComponent
import io.heimui.core.domain.model.component.HeimComponent
import io.heimui.core.domain.model.component.RadioGroupComponent
import io.heimui.core.domain.model.component.SelectComponent
import io.heimui.core.domain.model.component.SwitchComponent
import io.heimui.core.domain.model.component.TextFieldComponent
import io.heimui.core.presentation.component.heimContentColorFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The two rules a style override has to keep: a payload names a colour the host resolves per
 * theme rather than stating one, and every state a control needs is derived from that one name.
 */
class HeimControlStyleTest {

    private fun from(json: String): HeimComponent =
        HeimJson.instance.decodeFromString(HeimComponentDto.serializer(), json).toDomain()

    private fun buttonFrom(json: String): ButtonComponent =
        assertIs<ButtonComponent>(
            HeimJson.instance.decodeFromString(HeimComponentDto.serializer(), json).toDomain()
        )

    @Test
    fun `a button carries the style the payload named`() {
        val button = buttonFrom(
            """
            {
                "type": "button", "id": "pay", "title": "Pay",
                "background_color": "brand_primary",
                "text_color": "text_on_brand",
                "border_color": "outline",
                "border_width": 2,
                "corner_radius": 8
              }
            """.trimIndent()
        )

        // Carried through as names, not values: resolving them is the host app's job, and that is
        // what lets the same payload be one colour in light and another in dark.
        assertEquals("brand_primary", button.backgroundColor)
        assertEquals("text_on_brand", button.textColor)
        assertEquals("outline", button.borderColor)
        assertEquals(2, button.borderWidth)
        assertEquals(8, button.cornerRadius)
    }

    /** Every payload written before these existed has to render exactly as it did. */
    @Test
    fun `a button that says nothing about style leaves every override unset`() {
        val button = buttonFrom(
            """{"type":"button","id":"pay","title":"Pay"}"""
        )

        assertNull(button.backgroundColor)
        assertNull(button.textColor)
        assertNull(button.borderColor)
        assertNull(button.borderWidth)
        assertNull(button.cornerRadius)
    }

    /**
     * A negative dimension is not an instruction, and Compose throws on it -- one bad number would
     * cost the whole screen rather than one property.
     */
    @Test
    fun `negative dimensions are dropped rather than passed through`() {
        val button = buttonFrom(
            """{"type":"button","id":"p","title":"P","border_width":-4,"corner_radius":-1}"""
        )

        assertNull(button.borderWidth)
        assertNull(button.cornerRadius)
    }

    @Test
    fun `zero is a real value and survives`() {
        val button = buttonFrom(
            """{"type":"button","id":"p","title":"P","border_width":0,"corner_radius":0}"""
        )

        assertEquals(0, button.borderWidth)
        assertEquals(0, button.cornerRadius, "a square button is a design, not a mistake")
    }

    /** Name a fill and nothing else, and the label still has to be readable on it. */
    @Test
    fun `a foreground is derived that can be read on the background it sits on`() {
        assertEquals(Color.Black, heimContentColorFor(Color.White))
        assertEquals(Color.Black, heimContentColorFor(Color(0xFFF8FAFC)))
        assertEquals(Color.Black, heimContentColorFor(Color(0xFF00E5FF)), "cyan is a light colour")

        assertEquals(Color.White, heimContentColorFor(Color.Black))
        assertEquals(Color.White, heimContentColorFor(Color(0xFF0F172A)))
        assertEquals(Color.White, heimContentColorFor(Color(0xFF7C3AED)))
    }

    /** A styled button next to a field that cannot be touched is the same complaint, moved. */
    @Test
    fun `every interactive control carries the style the payload named`() {
        val field = assertIs<TextFieldComponent>(
            from(
                """
                {"type":"text_field","id":"n","state_key":"name",
                 "background_color":"surface_raised","text_color":"text_primary",
                 "border_color":"outline","border_width":2,"corner_radius":12,
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("surface_raised", field.backgroundColor)
        assertEquals("text_primary", field.textColor)
        assertEquals("outline", field.borderColor)
        assertEquals(2, field.borderWidth)
        assertEquals(12, field.cornerRadius)
        assertEquals("brand_primary", field.accentColor)

        val select = assertIs<SelectComponent>(
            from(
                """
                {"type":"select","id":"c","state_key":"country","placeholder":"Pick",
                 "options":[{"value":"CO","label":"Colombia"}],
                 "background_color":"surface_raised","text_color":"text_primary",
                 "border_color":"outline","border_width":1,"corner_radius":12,
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("surface_raised", select.backgroundColor)
        assertEquals("brand_primary", select.accentColor)
        assertEquals(12, select.cornerRadius)

        val date = assertIs<DatePickerComponent>(
            from(
                """
                {"type":"date_picker","id":"d","state_key":"born",
                 "background_color":"surface_raised","text_color":"text_primary",
                 "border_color":"outline","border_width":1,"corner_radius":12,
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("outline", date.borderColor)
        assertEquals("brand_primary", date.accentColor)

        val chip = assertIs<ChipComponent>(
            from(
                """
                {"type":"chip","id":"all","label":"All","variant":"FILTER",
                 "state_key":"category","value":"all",
                 "background_color":"surface_raised","text_color":"text_primary",
                 "border_color":"outline","border_width":1,"corner_radius":16,
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("surface_raised", chip.backgroundColor)
        assertEquals(16, chip.cornerRadius)
        // On a filter chip the accent is the selected fill, which is the state a designer
        // actually hands over a spec for.
        assertEquals("brand_primary", chip.accentColor)

        val switch = assertIs<SwitchComponent>(
            from(
                """
                {"type":"switch","id":"s","state_key":"push","label":"Push",
                 "text_color":"text_primary","background_color":"surface_raised",
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("text_primary", switch.textColor)
        assertEquals("surface_raised", switch.backgroundColor)
        assertEquals("brand_primary", switch.accentColor)

        val checkbox = assertIs<CheckboxComponent>(
            from(
                """
                {"type":"checkbox","id":"t","state_key":"accepted","label":"I accept",
                 "text_color":"text_primary","border_color":"outline",
                 "corner_radius":4,"accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("outline", checkbox.borderColor)
        assertEquals(4, checkbox.cornerRadius)
        assertEquals("brand_primary", checkbox.accentColor)

        val radio = assertIs<RadioGroupComponent>(
            from(
                """
                {"type":"radio_group","id":"d","state_key":"doc","label":"Document",
                 "options":[{"value":"cc","label":"Cedula"}],
                 "text_color":"text_primary","border_color":"outline",
                 "accent_color":"brand_primary"}
                """.trimIndent()
            )
        )
        assertEquals("text_primary", radio.textColor)
        assertEquals("outline", radio.borderColor)
        assertEquals("brand_primary", radio.accentColor)
    }

    /** The sanitising lives in the mapper, so it has to hold for whatever the mapper produced. */
    @Test
    fun `negative dimensions are dropped on every control that takes one`() {
        val field = assertIs<TextFieldComponent>(
            from(
                """{"type":"text_field","id":"n","state_key":"n",
                    "border_width":-1,"corner_radius":-8}""".trimIndent()
            )
        )
        assertNull(field.borderWidth)
        assertNull(field.cornerRadius)

        val chip = assertIs<ChipComponent>(
            from(
                """{"type":"chip","id":"c","label":"C","state_key":"k","value":"v",
                    "border_width":-2,"corner_radius":-3}""".trimIndent()
            )
        )
        assertNull(chip.borderWidth)
        assertNull(chip.cornerRadius)

        val checkbox = assertIs<CheckboxComponent>(
            from(
                """{"type":"checkbox","id":"t","state_key":"t","label":"T",
                    "corner_radius":-4}""".trimIndent()
            )
        )
        assertNull(checkbox.cornerRadius)
    }

    /** Silence still has to mean "whatever the app's theme says", on every one of them. */
    @Test
    fun `a payload written before any of this renders with nothing overridden`() {
        val switch = assertIs<SwitchComponent>(
            from("""{"type":"switch","id":"s","state_key":"push","label":"Push"}""")
        )
        assertNull(switch.textColor)
        assertNull(switch.backgroundColor)
        assertNull(switch.accentColor)

        val select = assertIs<SelectComponent>(
            from(
                """{"type":"select","id":"c","state_key":"c","placeholder":"Pick",
                    "options":[{"value":"CO","label":"Colombia"}]}""".trimIndent()
            )
        )
        assertNull(select.backgroundColor)
        assertNull(select.textColor)
        assertNull(select.borderColor)
        assertNull(select.borderWidth)
        assertNull(select.cornerRadius)
        assertNull(select.accentColor)

        val radio = assertIs<RadioGroupComponent>(
            from(
                """{"type":"radio_group","id":"d","state_key":"d","label":"D",
                    "options":[{"value":"cc","label":"Cedula"}]}""".trimIndent()
            )
        )
        assertNull(radio.textColor)
        assertNull(radio.borderColor)
        assertNull(radio.accentColor)
    }

    /**
     * A `card` could name its border colour and not its thickness, so the renderer picked 1dp for
     * everybody -- while a `box` sitting beside it took both. It surfaced as a save rejected for
     * "property 'border_width' is not defined in the schema", which is the schema being right
     * about a gap rather than the author being wrong.
     */
    @Test
    fun `a card takes a border width like the box beside it`() {
        val card = assertIs<CardComponent>(
            from(
                """
                {"type":"card","id":"plan","border_color":"outline","border_width":2,
                 "child":{"type":"text","id":"t","text":"Plan"}}
                """.trimIndent()
            )
        )

        assertEquals("outline", card.borderColor)
        assertEquals(2, card.borderWidth)
    }

    /** Every card written before this renders exactly as it did. */
    @Test
    fun `a card that says nothing about width keeps the one the renderer used to hardcode`() {
        val card = assertIs<CardComponent>(
            from("""{"type":"card","id":"c","child":{"type":"text","id":"t","text":"x"}}""")
        )
        assertEquals(1, card.borderWidth)
    }

    @Test
    fun `a negative card border is dropped rather than passed to Compose`() {
        val card = assertIs<CardComponent>(
            from(
                """{"type":"card","id":"c","border_width":-3,
                    "child":{"type":"text","id":"t","text":"x"}}""".trimIndent()
            )
        )
        assertEquals(0, card.borderWidth)
    }
}
