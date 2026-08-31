package io.heimui.core.domain.model

import io.heimui.core.domain.model.component.HeimComponent

data class HeimScreenResponse(
    val id: String,
    val version: String = "1.0.0",
    val title: String? = null,
    val applySafeInsets: Boolean = true,
    val root: HeimComponent,
    val metadata: Map<String, Any?>? = null,
    val signature: String? = null
)
