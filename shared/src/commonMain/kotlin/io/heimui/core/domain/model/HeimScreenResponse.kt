package io.heimui.core.domain.model

import io.heimui.core.domain.model.component.HeimComponent

public data class HeimScreenResponse(
    val id: String,
    val version: String = "1.0.0",
    val title: String? = null,
    val applySafeInsets: Boolean = true,
    val root: HeimComponent,
    val metadata: Map<String, HeimValue>? = null,
    val signature: String? = null,
    /**
     * Repairs the mapper had to make to render this payload (clamped dimensions, duplicate ids,
     * unknown component types, depth pruning). Empty for a well-formed payload. Surfaced as
     * telemetry so a backend team learns it is emitting invalid SDUI before users report it.
     */
    val violations: List<String> = emptyList()
)
