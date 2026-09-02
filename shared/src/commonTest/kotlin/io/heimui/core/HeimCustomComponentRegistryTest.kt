package io.heimui.core

import io.heimui.core.presentation.registry.HeimCustomComponentRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HeimCustomComponentRegistryTest {

    @Test
    fun `test register and retrieve custom component renderer`() {
        val registry = HeimCustomComponentRegistry.build {
            register("video_player") { _, _, _, _ -> }
            register("map_view") { _, _, _, _ -> }
        }

        assertNotNull(registry.getRenderer("video_player"))
        assertNotNull(registry.getRenderer("map_view"))
        assertNull(registry.getRenderer("unknown_component"))
    }
    @Test
    fun `registrations compose so a root does not grow with every component`() {
        // The registry returns itself, so registrations group into one extension function per
        // feature and the composition root stays a handful of lines however many components exist.
        fun HeimCustomComponentRegistry.productComponents() = apply {
            register("HORIZONTAL_CARD_PRODUCT") { _, _, _, _ -> }
            register("GRID_CARD_PRODUCT") { _, _, _, _ -> }
        }

        fun HeimCustomComponentRegistry.checkoutComponents() = apply {
            register("PAYMENT_METHOD_ROW") { _, _, _, _ -> }
        }

        val registry = HeimCustomComponentRegistry()
            .productComponents()
            .checkoutComponents()

        assertNotNull(registry.getRenderer("HORIZONTAL_CARD_PRODUCT"))
        assertNotNull(registry.getRenderer("GRID_CARD_PRODUCT"))
        assertNotNull(registry.getRenderer("PAYMENT_METHOD_ROW"))

        // A name nobody registered stays null rather than throwing: the renderer decides what to
        // do about it, and in production that is to draw nothing and report it.
        assertNull(registry.getRenderer("NEVER_REGISTERED"))
    }

}
