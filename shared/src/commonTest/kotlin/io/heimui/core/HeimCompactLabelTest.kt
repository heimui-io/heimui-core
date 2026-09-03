package io.heimui.core

import io.heimui.core.domain.model.component.BadgeComponent
import io.heimui.core.domain.model.component.ChipComponent
import io.heimui.core.data.dto.HeimComponentDto
import io.heimui.core.data.mapper.toDomain
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * A chip and a badge hold their label to one line, which is a rendering decision rather than a
 * payload one -- there is nothing in the schema to assert here, and there deliberately is not:
 * a chip of three lines is not a chip, so making it configurable would only make room for a bug.
 *
 * What a test can hold is the shape of the contract: the label travels as written, whole, and the
 * device decides where it runs out of room. Nothing truncates the string itself, so the same
 * payload ellipsises at a different word on a phone and on a tablet.
 */
class HeimCompactLabelTest {

    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    private fun from(raw: String) = json.decodeFromString(HeimComponentDto.serializer(), raw).toDomain()

    private val long = "Electrodomesticos cocina y pequeno hogar"

    @Test
    fun `a chip carries its whole label however long it is`() {
        val chip = assertIs<ChipComponent>(
            from("""{"type":"chip","id":"c","label":"$long"}""")
        )
        assertEquals(long, chip.label, "truncation belongs to layout, not to the payload")
    }

    @Test
    fun `a badge carries its whole text`() {
        val badge = assertIs<BadgeComponent>(
            from("""{"type":"badge","id":"b","text":"$long"}""")
        )
        assertEquals(long, badge.text)
    }
}
