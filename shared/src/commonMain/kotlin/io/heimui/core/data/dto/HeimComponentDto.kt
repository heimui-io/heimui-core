package io.heimui.core.data.dto

import io.heimui.core.data.serialization.LenientBooleanSerializer
import io.heimui.core.data.serialization.LenientNullableBooleanSerializer
import io.heimui.core.data.serialization.LenientIntSerializer
import io.heimui.core.data.serialization.LenientNullableFloatSerializer
import io.heimui.core.data.serialization.LenientNullableIntSerializer
import io.heimui.core.domain.model.component.HeimPadding
import io.heimui.core.domain.model.component.HeimSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire format of a HeimUI component.
 *
 * The `type` field is the polymorphic discriminator: `"container"`, `"text"`, `"button"`, and so
 * on. A type this SDK version does not know deserialises into [UnknownComponentDto] instead of
 * failing the whole screen, which is what lets a server ship a new component to clients that have
 * not been updated yet.
 *
 * These DTOs mirror the JSON exactly (snake_case field names). The rendering layer never sees
 * them: [io.heimui.core.data.mapper.toDomain] maps them to the domain model, clamping
 * out-of-range values along the way.
 */
@Serializable
public sealed interface HeimComponentDto {
    public val id: String
    public val visibleIf: String? get() = null

    /** See `HeimComponent.stateScope`. Written by hydration, ignored by older clients. */
    public val stateScope: String? get() = null
    public val a11y: HeimAccessibilityDto? get() = null

    /**
     * Share of the parent's main axis this component claims, when the parent is a `container`.
     *
     * Two cards with `weight: 1` split a row in half; one with `2` and one with `1` split it two
     * thirds to one third. Ignored outside a container, because there is no axis to divide.
     *
     * Declared here rather than per component so the parent can apply it uniformly — a size is a
     * relationship between a child and its parent, not a property of the child alone.
     */
    public val weight: Float? get() = null

    /**
     * Intrinsic dimensions this component asks for. See
     * [io.heimui.core.domain.model.component.HeimSize] for which of them is safe to reach for.
     */
    public val frame: SizeSpec get() = HeimSize.None
}

@Serializable
public enum class DirectionDto {
    @SerialName("VERTICAL") VERTICAL,
    @SerialName("HORIZONTAL") HORIZONTAL
}

@Serializable
public enum class ArrangementDto {
    @SerialName("PACKED") PACKED,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("SPACE_BETWEEN") SPACE_BETWEEN,
    @SerialName("SPACE_AROUND") SPACE_AROUND,
    @SerialName("SPACE_EVENLY") SPACE_EVENLY
}

@Serializable
public enum class AlignmentDto {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("TOP") TOP,
    @SerialName("BOTTOM") BOTTOM
}

@Serializable
public enum class TextAlignDto {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("JUSTIFY") JUSTIFY
}

@Serializable
public enum class ContentScaleDto {
    @SerialName("CROP") CROP,
    @SerialName("FIT") FIT,
    @SerialName("FILL_BOUNDS") FILL_BOUNDS,
    @SerialName("INSIDE") INSIDE
}

@Serializable
public enum class ButtonVariantDto {
    @SerialName("FILLED") FILLED,
    @SerialName("OUTLINED") OUTLINED,
    @SerialName("TEXT") TEXT,
    @SerialName("TONAL") TONAL
}

@Serializable
public enum class InputTypeDto {
    @SerialName("TEXT") TEXT,
    @SerialName("NUMBER") NUMBER,
    @SerialName("EMAIL") EMAIL,
    @SerialName("PASSWORD") PASSWORD,
    @SerialName("PHONE") PHONE
}

@Serializable
@SerialName("container")
public data class ContainerComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    val direction: DirectionDto = DirectionDto.VERTICAL,
    val alignment: AlignmentDto = AlignmentDto.START,
    val arrangement: ArrangementDto = ArrangementDto.PACKED,
    val padding: PaddingSpec = HeimPadding.None,
    @Serializable(with = LenientIntSerializer::class) val spacing: Int = 0,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("corner_radius") @Serializable(with = LenientIntSerializer::class) val cornerRadius: Int = 0,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") @Serializable(with = LenientIntSerializer::class) val borderWidth: Int = 1,
    @SerialName("scrollable") @Serializable(with = LenientNullableBooleanSerializer::class) val scrollable: Boolean? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val children: List<HeimComponentDto> = emptyList(),
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("box")
public data class BoxComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    @SerialName("content_alignment") val contentAlignment: AlignmentDto = AlignmentDto.CENTER,
    val padding: PaddingSpec = HeimPadding.None,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("corner_radius") @Serializable(with = LenientIntSerializer::class) val cornerRadius: Int = 0,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") @Serializable(with = LenientIntSerializer::class) val borderWidth: Int = 1,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val children: List<HeimComponentDto> = emptyList(),
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("lazy_column")
public data class LazyColumnComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @Serializable(with = LenientIntSerializer::class) val spacing: Int = 8,
    val padding: PaddingSpec = HeimPadding.None,
    val alignment: AlignmentDto = AlignmentDto.START,
    val arrangement: ArrangementDto = ArrangementDto.PACKED,
    val items: List<HeimComponentDto> = emptyList(),
    val pagination: PaginationConfigDto? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("lazy_row")
public data class LazyRowComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @Serializable(with = LenientIntSerializer::class) val spacing: Int = 8,
    val padding: PaddingSpec = HeimPadding.None,
    val alignment: AlignmentDto = AlignmentDto.START,
    val arrangement: ArrangementDto = ArrangementDto.PACKED,
    val items: List<HeimComponentDto> = emptyList(),
    val pagination: PaginationConfigDto? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
public data class PaginationConfigDto(
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") @Serializable(with = LenientBooleanSerializer::class) val hasMore: Boolean = false,
    @SerialName("load_threshold") @Serializable(with = LenientIntSerializer::class) val loadThreshold: Int = 3,
    @SerialName("on_load_more_actions") val onLoadMoreActions: List<HeimActionDto> = emptyList()
)

@Serializable
@SerialName("text")
public data class TextComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val text: String = "",
    val style: String = "bodyMedium",
    val color: String? = null,
    @SerialName("max_lines") @Serializable(with = LenientNullableIntSerializer::class) val maxLines: Int? = null,
    @SerialName("text_align") val textAlign: TextAlignDto = TextAlignDto.START,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("image")
public data class ImageComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val url: String = "",
    @SerialName("blur_hash") val blurHash: String? = null,
    @SerialName("aspect_ratio") @Serializable(with = LenientNullableFloatSerializer::class) val aspectRatio: Float? = null,
    @Serializable(with = LenientNullableIntSerializer::class) val height: Int? = null,
    @SerialName("corner_radius") @Serializable(with = LenientIntSerializer::class) val cornerRadius: Int = 0,
    @SerialName("content_scale") val contentScale: ContentScaleDto = ContentScaleDto.CROP,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("card")
public data class CardComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    @Serializable(with = LenientIntSerializer::class) val elevation: Int = 0,
    @SerialName("corner_radius") @Serializable(with = LenientIntSerializer::class) val cornerRadius: Int = 12,
    @SerialName("background_color") val backgroundColor: String = "surface",
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") @Serializable(with = LenientIntSerializer::class) val borderWidth: Int = 1,
    val padding: PaddingSpec = HeimPadding.all(12),
    val actions: List<HeimActionDto> = emptyList(),
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val child: HeimComponentDto = UnknownComponentDto(id = "missing_child"),
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
public enum class ChipVariantDto {
    @SerialName("ASSIST") ASSIST,
    @SerialName("FILTER") FILTER
}

/**
 * A compact, tappable label — a category pill, a tag, a filter.
 *
 * Distinct from `badge`, which is decoration and cannot be tapped, and from `button`, which is
 * the wrong size and shape for a strip of them. It is the most common thing an SDUI payload wants
 * that neither of the other two can express.
 *
 * Bind it to state and it becomes a filter:
 * - `state_key` **with** `value` — one of a group, like a radio. Chips sharing a key are mutually
 *   exclusive, and tapping the selected one clears it.
 * - `state_key` **without** `value` — an independent on/off, for a multi-select row.
 *
 * Leave `state_key` out and it is a plain action chip.
 */
@Serializable
@SerialName("chip")
public data class ChipComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val label: String = "",
    val icon: String? = null,
    val variant: ChipVariantDto = ChipVariantDto.ASSIST,
    @SerialName("state_key") val stateKey: String? = null,
    val value: String? = null,
    @SerialName("is_enabled") @Serializable(with = LenientBooleanSerializer::class) val isEnabled: Boolean = true,
    val actions: List<HeimActionDto> = emptyList(),
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") val borderWidth: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

/**
 * One run of text inside a [RichTextComponentDto].
 *
 * A [url] turns the span into a link. It goes through the same `HeimUrlLauncher` and scheme
 * policy as `open_url`, so a payload cannot smuggle `javascript:` or `intent://` into a paragraph
 * just because it wrote it as a link instead of an action.
 */
@Serializable
public data class HeimTextSpanDto(
    val text: String = "",
    val style: String? = null,
    val color: String? = null,
    val weight: String? = null,
    val url: String? = null
)

/**
 * Text made of styled runs, where one paragraph can mix weights, colours and links.
 *
 * The component a regulated onboarding needs and `text` cannot express: "I accept the
 * [terms and conditions]" with only the bracketed part linked. Splitting it into three components
 * puts a line break where the sentence should flow, and inlining HTML would hand the payload a
 * renderer it should never have.
 */
@Serializable
@SerialName("rich_text")
public data class RichTextComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val spans: List<HeimTextSpanDto> = emptyList(),
    val style: String = "bodyMedium",
    val color: String? = null,
    val align: TextAlignDto = TextAlignDto.START,
    @SerialName("max_lines") @Serializable(with = LenientIntSerializer::class) val maxLines: Int = 0,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("badge")
public data class BadgeComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val text: String = "",
    @SerialName("background_color") val backgroundColor: String = "primaryContainer",
    @SerialName("text_color") val textColor: String = "onPrimaryContainer",
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("button")
public data class ButtonComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val title: String = "",
    val variant: ButtonVariantDto = ButtonVariantDto.FILLED,
    @SerialName("is_full_width") @Serializable(with = LenientBooleanSerializer::class) val isFullWidth: Boolean = false,
    @SerialName("is_enabled") @Serializable(with = LenientBooleanSerializer::class) val isEnabled: Boolean = true,
    @SerialName("is_loading") @Serializable(with = LenientBooleanSerializer::class) val isLoading: Boolean = false,
    val icon: String? = null,
    val actions: List<HeimActionDto> = emptyList(),
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") val borderWidth: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("text_field")
public data class TextFieldComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    @SerialName("input_type") val inputType: InputTypeDto = InputTypeDto.TEXT,
    @SerialName("initial_value") val initialValue: String = "",
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("helper_text") val helperText: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") val borderWidth: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

/**
 * One choice in a `radio_group` or a `select`.
 *
 * [value] is what lands in form state and travels to the backend; [label] is what the user reads.
 * Keeping them separate is what lets the label be translated without the stored value changing.
 */
@Serializable
public data class HeimOptionDto(
    val value: String = "",
    val label: String = ""
)

@Serializable
@SerialName("checkbox")
public data class CheckboxComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String = "",
    @SerialName("initial_checked") @Serializable(with = LenientBooleanSerializer::class) val initialChecked: Boolean = false,
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("on_check_actions") val onCheckActions: List<HeimActionDto> = emptyList(),
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("radio_group")
public data class RadioGroupComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String? = null,
    val options: List<HeimOptionDto> = emptyList(),
    @SerialName("initial_value") val initialValue: String = "",
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("on_select_actions") val onSelectActions: List<HeimActionDto> = emptyList(),
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("radio")
public data class RadioComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val value: String = "true",
    val label: String? = null,
    @SerialName("initial_selected") @Serializable(with = LenientBooleanSerializer::class) val initialSelected: Boolean = false,
    @SerialName("on_select_actions") val onSelectActions: List<HeimActionDto> = emptyList(),
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("select")
public data class SelectComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val options: List<HeimOptionDto> = emptyList(),
    @SerialName("initial_value") val initialValue: String = "",
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("on_select_actions") val onSelectActions: List<HeimActionDto> = emptyList(),
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") val borderWidth: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

/**
 * Date input. State is stored as ISO-8601 `YYYY-MM-DD`.
 *
 * ISO rather than a localised string on purpose: the value travels to a backend, and `15/03/2024`
 * is 15 March in Bogotá and nothing at all in a parser expecting month first. The *display* is
 * localised by the client; the stored value never is.
 */
@Serializable
@SerialName("date_picker")
public data class DatePickerComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    @SerialName("initial_value") val initialValue: String = "",
    @SerialName("min_date") val minDate: String? = null,
    @SerialName("max_date") val maxDate: String? = null,
    @SerialName("confirm_text") val confirmText: String = "OK",
    @SerialName("dismiss_text") val dismissText: String = "Cancel",
    @SerialName("validation_rules") val validationRules: List<ValidationRuleDto> = emptyList(),
    @SerialName("on_select_actions") val onSelectActions: List<HeimActionDto> = emptyList(),
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("border_width") val borderWidth: Int? = null,
    @SerialName("corner_radius") val cornerRadius: Int? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("switch")
public data class SwitchComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("state_key") val stateKey: String = "",
    val label: String = "",
    @SerialName("initial_checked") @Serializable(with = LenientBooleanSerializer::class) val initialChecked: Boolean = false,
    @SerialName("on_check_actions") val onCheckActions: List<HeimActionDto> = emptyList(),
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("icon")
public data class IconComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val name: String = "",
    val tint: String? = null,
    @Serializable(with = LenientIntSerializer::class) val size: Int = 24,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("spacer")
public data class SpacerComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @Serializable(with = LenientIntSerializer::class) val size: Int = 0,
    @SerialName("is_flexible") @Serializable(with = LenientBooleanSerializer::class) val isFlexible: Boolean = false,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("divider")
public data class DividerComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @Serializable(with = LenientIntSerializer::class) val thickness: Int = 1,
    val color: String = "outlineVariant",
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("custom")
public data class CustomComponentDto(
    override val id: String = "",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    val name: String = "",
    val data: JsonObject? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto

@Serializable
@SerialName("unknown")
public data class UnknownComponentDto(
    override val id: String = "unknown_component",
    @SerialName("visible_if") override val visibleIf: String? = null,
    override val a11y: HeimAccessibilityDto? = null,
    override val weight: Float? = null,
    override val frame: SizeSpec = HeimSize.None,
    @SerialName("original_type") val originalType: String? = null,
    @SerialName("state_scope") override val stateScope: String? = null,
) : HeimComponentDto
