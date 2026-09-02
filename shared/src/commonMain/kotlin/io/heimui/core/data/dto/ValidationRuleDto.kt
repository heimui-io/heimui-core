package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ValidationRuleDto(
    val type: ValidationTypeDto,
    val value: String? = null,
    /**
     * Shown when the rule fails.
     *
     * Defaulted rather than required, and that is a correction. It used to be mandatory, so a
     * payload that forgot it failed **the whole screen** — one missing string cost every field,
     * every button and the content around them. That contradicts the rule the rest of the SDK
     * follows: repair what can be repaired, and never let a cosmetic defect become an outage.
     *
     * An empty message still blocks submission, which is the part that protects the backend. The
     * user just gets no explanation, and the payload author gets a `PayloadViolation` telling
     * them why.
     */
    @SerialName("error_message") val errorMessage: String = ""
)

@Serializable
public enum class ValidationTypeDto {
    @SerialName("REQUIRED") REQUIRED,
    @SerialName("REGEX") REGEX,
    @SerialName("MIN_LENGTH") MIN_LENGTH,
    @SerialName("MAX_LENGTH") MAX_LENGTH,
    @SerialName("EMAIL") EMAIL,
    @SerialName("NUMERIC") NUMERIC,
    @SerialName("CUSTOM") CUSTOM
}
