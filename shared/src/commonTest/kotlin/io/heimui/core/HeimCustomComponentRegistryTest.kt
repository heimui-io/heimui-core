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
}
