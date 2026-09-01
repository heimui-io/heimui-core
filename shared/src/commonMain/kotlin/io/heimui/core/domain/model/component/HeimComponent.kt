package io.heimui.core.domain.model.component

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.validation.ValidationRule

public sealed interface HeimComponent {
    public val id: String
    public val visibleIf: String? get() = null
    public val a11y: HeimAccessibility? get() = null
}

public enum class Direction {
    VERTICAL,
    HORIZONTAL
}

public enum class Alignment {
    START,
    CENTER,
    END,
    TOP,
    BOTTOM
}

public enum class TextAlign {
    START,
    CENTER,
    END,
    JUSTIFY
}

public enum class ContentScale {
    CROP,
    FIT,
    FILL_BOUNDS,
    INSIDE
}

public enum class ButtonVariant {
    FILLED,
    OUTLINED,
    TEXT,
    TONAL
}

public enum class InputType {
    TEXT,
    NUMBER,
    EMAIL,
    PASSWORD,
    PHONE
}

/**
 * Linear layout. Stacks [children] along [direction].
 *
 * Vertical containers scroll by default; set [scrollable] to `false` when nesting one inside
 * another scrolling container, which would otherwise fail to measure.
 *
 * @property direction axis the children are laid out along.
 * @property alignment cross-axis alignment of the children.
 * @property padding inner padding in dp. Negative values are clamped to 0.
 * @property spacing gap between children in dp. Negative values are clamped to 0.
 * @property backgroundColor design token or `#RRGGBB` hex. `null` is transparent.
 */
public data class ContainerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val direction: Direction = Direction.VERTICAL,
    val alignment: Alignment = Alignment.START,
    val padding: Int = 0,
    val spacing: Int = 0,
    val backgroundColor: String? = null,
    /** Vertical containers scroll by default; set false when nesting inside another scroller. */
    val scrollable: Boolean = true,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

/**
 * Overlay layout. Draws [children] stacked on the z-axis, each positioned by [contentAlignment].
 * Use it for badges over images, or a loading veil over content.
 */
public data class BoxComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val contentAlignment: Alignment = Alignment.CENTER,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

/**
 * Vertically scrolling list that only composes visible items.
 *
 * Prefer this over a [ContainerComponent] for long or paginated collections. Item ids must be
 * unique within the list; duplicates are disambiguated during mapping rather than crashing.
 *
 * @property pagination cursor and actions for loading the next page. `null` for a fixed list.
 */
public data class LazyColumnComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

/**
 * Horizontally scrolling carousel that only composes visible items. See [LazyColumnComponent].
 */
public data class LazyRowComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

public data class PaginationConfig(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadThreshold: Int = 3,
    val onLoadMoreActions: List<HeimAction> = emptyList()
)

/**
 * A run of text.
 *
 * @property style typography token, e.g. `"titleLarge"` or `"bodyMedium"`, resolved against the
 *   Material type scale or a registered brand token.
 * @property color design token or hex. `null` uses the on-surface color.
 * @property maxLines truncates with an ellipsis beyond this many lines. `null` is unlimited;
 *   values below 1 are treated as unlimited.
 */
public data class TextComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val text: String,
    val style: String = "bodyMedium",
    val color: String? = null,
    val maxLines: Int? = null,
    val textAlign: TextAlign = TextAlign.START
) : HeimComponent

/**
 * Remote image with an optional BlurHash placeholder.
 *
 * @property url image source. Only schemes permitted by the loader are fetched.
 * @property blurHash compact placeholder shown while loading. See https://blurha.sh.
 * @property aspectRatio width/height. Ignored when [height] is set, or when not positive.
 * @property height fixed height in dp. Takes precedence over [aspectRatio].
 */
public data class ImageComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val url: String,
    val blurHash: String? = null,
    val aspectRatio: Float? = null,
    val height: Int? = null,
    val cornerRadius: Int = 0,
    val contentScale: ContentScale = ContentScale.CROP
) : HeimComponent

/**
 * Elevated surface wrapping a single [child].
 *
 * When [actions] is non-empty the whole card becomes clickable.
 */
public data class CardComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val elevation: Int = 0,
    val cornerRadius: Int = 12,
    val backgroundColor: String = "surface",
    val borderColor: String? = null,
    val padding: Int = 12,
    val actions: List<HeimAction> = emptyList(),
    val child: HeimComponent
) : HeimComponent

/** Compact pill-shaped label, for statuses, counts and tags. */
public data class BadgeComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val text: String,
    val backgroundColor: String = "primaryContainer",
    val textColor: String = "onPrimaryContainer",
    val iconUrl: String? = null
) : HeimComponent

/**
 * Tappable button that dispatches [actions] when pressed.
 *
 * @property isLoading replaces the label with a spinner and blocks interaction.
 */
public data class ButtonComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val title: String,
    val variant: ButtonVariant = ButtonVariant.FILLED,
    val isFullWidth: Boolean = false,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val actions: List<HeimAction> = emptyList()
) : HeimComponent

/**
 * Text input bound to the form state under [stateKey].
 *
 * Rules in [validationRules] run as the user types **and** again before submission, so a field
 * the user never touched still blocks an invalid form. `PASSWORD` inputs are excluded from draft
 * persistence automatically.
 *
 * @property stateKey key this field reads and writes in the screen state; also the key used by
 *   `{{state.key}}` interpolation in a submit payload.
 * @property helperText shown below the field while there is no validation error.
 */
public data class TextFieldComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val stateKey: String,
    val label: String? = null,
    val placeholder: String? = null,
    val inputType: InputType = InputType.TEXT,
    val initialValue: String = "",
    val validationRules: List<ValidationRule> = emptyList(),
    val helperText: String? = null
) : HeimComponent

/** Boolean toggle bound to [stateKey], dispatching [onCheckActions] on every change. */
public data class SwitchComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val stateKey: String,
    val label: String,
    val initialChecked: Boolean = false,
    val onCheckActions: List<HeimAction> = emptyList()
) : HeimComponent

/**
 * Vector icon drawn by name.
 *
 * An unrecognised name falls back to a generic glyph and emits a telemetry event, so a typo in a
 * payload is visible rather than silent.
 */
public data class IconComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val tint: String? = null,
    val size: Int = 24
) : HeimComponent

/**
 * Empty space.
 *
 * @property isFlexible when true, absorbs the remaining space along the parent's axis instead of
 *   using a fixed [size].
 */
public data class SpacerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val size: Int,
    val isFlexible: Boolean = false
) : HeimComponent

/** Horizontal rule separating sections. */
public data class DividerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val thickness: Int = 1,
    val color: String = "outlineVariant"
) : HeimComponent

/**
 * Escape hatch for host-provided native components.
 *
 * Resolved against the component registry by [name]; [data] carries arbitrary typed parameters.
 * Renders a labelled placeholder when nothing is registered under that name.
 */
public data class CustomComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val data: Map<String, HeimValue> = emptyMap()
) : HeimComponent

/**
 * Fallback for a component type this SDK version does not recognise.
 *
 * Produced when a newer server sends a type an older client cannot render. The surrounding tree
 * still renders, and [originalType] carries the unrecognised name for telemetry.
 */
public data class UnknownComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val originalType: String? = null
) : HeimComponent
