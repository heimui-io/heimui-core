package io.heimui.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface HeimComponentDto {
    val id: String
    val visibleIf: String? get() = null
    val a11y: HeimAccessibilityDto? get() = null
}

@Serializable
enum class DirectionDto {
    @SerialName("VERTICAL") VERTICAL,
    @SerialName("HORIZONTAL") HORIZONTAL
}

@Serializable
enum class AlignmentDto {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("TOP") TOP,
    @SerialName("BOTTOM") BOTTOM
}

@Serializable
enum class TextAlignDto {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("JUSTIFY") JUSTIFY
}

@Serializable
enum class ContentScaleDto {
    @SerialName("CROP") CROP,
    @SerialName("FIT") FIT,
    @SerialName("FILL_BOUNDS") FILL_BOUNDS,
    @SerialName("INSIDE") INSIDE
}

@Serializable
enum class ButtonVariantDto {
    @SerialName("FILLED") FILLED,
    @SerialName("OUTLINED") OUTLINED,
    @SerialName("TEXT") TEXT,
    @SerialName("TONAL") TONAL
}

@Serializable
enum class InputTypeDto {
    @SerialName("TEXT") TEXT,
    @SerialName("NUMBER") NUMBER,
    @SerialName("EMAIL") EMAIL,
    @SerialName("PASSWORD") PASSWORD,
    @SerialName("PHONE") PHONE
}

// 1. Container
@Serializable
@SerialName("container")
data class ContainerComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val direction: DirectionDto = DirectionDto.VERTICAL,
    val alignment: AlignmentDto = AlignmentDto.START,
    val padding: Int = 0,
    val spacing: Int = 0,
    @SerialName("background_color") val backgroundColor: String? = null,
    val children: List<HeimComponentDto> = emptyList()
) : HeimComponentDto

// 2. Box
@Serializable
@SerialName("box")
data class BoxComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    @SerialName("content_alignment") val contentAlignment: AlignmentDto = AlignmentDto.CENTER,
    val children: List<HeimComponentDto> = emptyList()
) : HeimComponentDto

// 3. LazyColumn
@Serializable
@SerialName("lazy_column")
data class LazyColumnComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponentDto> = emptyList(),
    val pagination: PaginationConfigDto? = null
) : HeimComponentDto

// 4. LazyRow
@Serializable
@SerialName("lazy_row")
data class LazyRowComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponentDto> = emptyList(),
    val pagination: PaginationConfigDto? = null
) : HeimComponentDto

@Serializable
data class PaginationConfigDto(
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("load_threshold") val loadThreshold: Int = 3,
    @SerialName("on_load_more_actions") val onLoadMoreActions: List<HeimActionDto> = emptyList()
)

// 5. Text
@Serializable
@SerialName("text")
data class TextComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val text: String,
    val style: String = "bodyMedium",
    val color: String? = null,
    @SerialName("max_lines") val maxLines: Int? = null,
    @SerialName("text_align") val textAlign: TextAlignDto = TextAlignDto.START
) : HeimComponentDto

// 6. Image
@Serializable
@SerialName("image")
data class ImageComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val url: String,
    @SerialName("blur_hash") val blurHash: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: Float? = null,
    val height: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int = 0,
    @SerialName("content_scale") val contentScale: ContentScaleDto = ContentScaleDto.CROP
) : HeimComponentDto

// 7. Card
@Serializable
@SerialName("card")
data class CardComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val elevation: Int = 0,
    @SerialName("corner_radius") val cornerRadius: Int = 12,
    @SerialName("background_color") val backgroundColor: String = "surface",
    @SerialName("border_color") val borderColor: String? = null,
    val padding: Int = 12,
    val actions: List<HeimActionDto> = emptyList(),
    val child: HeimComponentDto
) : HeimComponentDto

// 8. Badge
@Serializable
@SerialName("badge")
data class BadgeComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val text: String,
    @SerialName("background_color") val backgroundColor: String = "primaryContainer",
    @SerialName("text_color") val textColor: String = "onPrimaryContainer",
    @SerialName("icon_url") val iconUrl: String? = null
) : HeimComponentDto

// 9. Button
@Serializable
@SerialName("button")
data class ButtonComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val title: String,
    val variant: ButtonVariantDto = ButtonVariantDto.FILLED,
    @SerialName("is_full_width") val isFullWidth: Boolean = false,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_loading") val isLoading: Boolean = false,
    val actions: List<HeimActionDto> = emptyList()
) : HeimComponentDto

// 10. TextField
@Serializable
@SerialName("text_field")
data class TextFieldComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    @SerialName("state_key") val stateKey: String,
    val label: String? = null,
    val placeholder: String? = null,
    @SerialName("input_type") val inputType: InputTypeDto = InputTypeDto.TEXT,
    @SerialName("initial_value") val initialValue: String = "",
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("helper_text") val helperText: String? = null
) : HeimComponentDto

// 11. Switch
@Serializable
@SerialName("switch")
data class SwitchComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    @SerialName("state_key") val stateKey: String,
    val label: String,
    @SerialName("initial_checked") val initialChecked: Boolean = false,
    @SerialName("on_check_actions") val onCheckActions: List<HeimActionDto> = emptyList()
) : HeimComponentDto

// 12. Icon
@Serializable
@SerialName("icon")
data class IconComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val name: String,
    val tint: String? = null,
    val size: Int = 24
) : HeimComponentDto

// 13. Spacer
@Serializable
@SerialName("spacer")
data class SpacerComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val size: Int,
    @SerialName("is_flexible") val isFlexible: Boolean = false
) : HeimComponentDto

// 14. Divider
@Serializable
@SerialName("divider")
data class DividerComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val thickness: Int = 1,
    val color: String = "outlineVariant"
) : HeimComponentDto

// 15. Custom & Unknown
@Serializable
@SerialName("custom")
data class CustomComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val name: String,
    val data: JsonObject? = null
) : HeimComponentDto

@Serializable
@SerialName("unknown")
data class UnknownComponentDto(
    override val id: String,
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null
) : HeimComponentDto
