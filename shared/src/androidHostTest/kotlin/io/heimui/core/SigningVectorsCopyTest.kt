package io.heimui.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The vectors embedded for the common tests are the ones published in `schema/signing`.
 *
 * Only here, because only a JVM test can open the file. The published copy is what the Studio, the
 * reference backend and the docs test against; an embedded copy that drifted would let this SDK
 * pass a set of cases nobody else is held to.
 */
class SigningVectorsCopyTest {

    @Test
    fun `the embedded vectors are the published ones`() {
        val start = File("").absoluteFile
        val published = generateSequence(start) { it.parentFile }
            .map { it.resolve("schema/signing/es256-vectors.json") }
            .firstOrNull { it.isFile }
            ?: error("schema/signing/es256-vectors.json was not found above $start")

        assertEquals(published.readText().trim(), SIGNING_VECTORS_JSON.trim())
    }
}
