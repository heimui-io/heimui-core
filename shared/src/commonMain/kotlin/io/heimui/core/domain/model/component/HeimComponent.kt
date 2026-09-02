package io.heimui.core.domain.model.component

import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.validation.ValidationRule

public sealed interface HeimComponent {
    public val id: String
    public val visibleIf: String? get() = null
    public val a11y: HeimAccessibility? get() = null

    /**
     * Share of the parent's main axis this component claims, when the parent is a
     * [ContainerComponent].
     *
     * Two cards with `1f` split a row in half; `2f` and `1f` split it two thirds to one third.
     * Ignored outside a container, because there is no axis to divide — and ignored on a
     * container's cross axis for the same reason.
     *
     * Lives on the interface rather than per component because a size is a relationship between
     * a child and its parent, so the parent is what applies it.
     */
    public val weight: Float? get() = null

    /**
     * Intrinsic dimensions this component asks for. See [HeimSize].
     *
     * Named `frame` after SwiftUI rather than `size`, because `size` is already taken by `spacer`
     * and `icon` as a scalar length. Renaming two established keys to free the word would break
     * every payload using them, for no gain a reader would notice.
     */
    public val frame: HeimSize get() = HeimSize.None
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
 * ### Padding here scrolls with the content
 * A container's [padding] is applied to the container's own box, *inside* the scroll viewport.
 * The first child therefore starts `start` dp in and that gap scrolls away with everything else.
 *
 * That is the right behaviour for a form or a page, and the wrong one for a chip strip or a
 * carousel, where the row should scroll edge to edge while keeping a fixed inset at both ends.
 * For those use [LazyRowComponent], whose padding becomes Compose's `contentPadding` and so sits
 * outside the scrolling area. This is the same distinction other design systems draw by naming
 * the two separately — a `padding` that clips and an `insets` that does not.
 *
 * ### Scrolling
 * Both axes scroll by default. Set [scrollable] to `false` when nesting inside another scroller
 * on the same axis, which would otherwise fail to measure against infinite constraints.
 *
 * @property direction axis the children are laid out along.
 * @property alignment cross-axis alignment of the children.
 * @property padding inner padding in dp, per side. Negative values are clamped to 0.
 * @property spacing gap between children in dp. Negative values are clamped to 0.
 * @property backgroundColor design token or `#RRGGBB` hex. `null` is transparent.
 */
/**
 * How children are distributed along a container's own axis.
 *
 * Distinct from `alignment`, which positions them across the *other* axis. A vertical container's
 * `alignment` decides whether children sit left, centre or right; its [arrangement] decides
 * whether they stack at the top, sit centred, or spread to fill the height.
 *
 * [PACKED] keeps the existing behaviour — children separated by `spacing` and packed at the
 * start — so a payload that never mentions arrangement lays out exactly as before.
 */
public enum class HeimArrangement {
    /** Separated by `spacing`, packed at the start of the axis. The default. */
    PACKED,
    /** Packed as a group at the centre of the axis. */
    CENTER,
    /** Packed as a group at the end of the axis. */
    END,
    /** First and last flush to the edges, remaining space split evenly between children. */
    SPACE_BETWEEN,
    /** Equal space around each child, so edge gaps are half the gaps between children. */
    SPACE_AROUND,
    /** Equal space between and around every child. */
    SPACE_EVENLY,
}

public data class ContainerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val direction: Direction = Direction.VERTICAL,
    val alignment: Alignment = Alignment.START,
    val arrangement: HeimArrangement = HeimArrangement.PACKED,
    val padding: HeimPadding = HeimPadding.None,
    val spacing: Int = 0,
    val backgroundColor: String? = null,
    /** Scrolls along [direction] by default; set false when nesting inside another scroller. */
    val scrollable: Boolean = true,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    val padding: HeimPadding = HeimPadding.None,
    val backgroundColor: String? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

/**
 * Vertically scrolling list that only composes visible items.
 *
 * Prefer this over a [ContainerComponent] for long or paginated collections. Item ids must be
 * unique within the list; duplicates are disambiguated during mapping rather than crashing.
 *
 * [padding] becomes Compose's `contentPadding`: it insets the items but stays *outside* the
 * scrolling viewport, so the first and last items keep their gap while the list still scrolls
 * edge to edge. A [ContainerComponent]'s padding scrolls away with the content instead — the
 * distinction matters, and it is why the same key behaves differently on the two.
 *
 * @property pagination cursor and actions for loading the next page. `null` for a fixed list.
 */
public data class LazyColumnComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val spacing: Int = 8,
    val padding: HeimPadding = HeimPadding.None,
    val alignment: Alignment = Alignment.START,
    val arrangement: HeimArrangement = HeimArrangement.PACKED,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

/**
 * Horizontally scrolling carousel that only composes visible items. See [LazyColumnComponent].
 *
 * This is the right component for a chip strip or a category rail. [padding] is `contentPadding`,
 * so `{ "horizontal": 16 }` keeps a 16dp inset at both ends while the items still scroll all the
 * way to the screen edge — rather than clipping them 16dp early, which is what a padded
 * [ContainerComponent] would do.
 */
public data class LazyRowComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val spacing: Int = 8,
    val padding: HeimPadding = HeimPadding.None,
    val alignment: Alignment = Alignment.START,
    val arrangement: HeimArrangement = HeimArrangement.PACKED,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    val padding: HeimPadding = HeimPadding.all(12),
    val actions: List<HeimAction> = emptyList(),
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val child: HeimComponent
) : HeimComponent

/** How a chip reads: an action to take, or a filter that can be on. */
public enum class ChipVariant { ASSIST, FILTER }

/**
 * Compact tappable label — a category pill, a tag, a filter.
 *
 * Fills the gap between [BadgeComponent], which cannot be tapped, and [ButtonComponent], which is
 * the wrong shape for a strip of them.
 *
 * @property stateKey binds it to form state. With [value] the chips sharing a key behave as one
 *   choice; without it the chip is an independent on/off. Null makes it a plain action chip.
 */
public data class ChipComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val label: String,
    val icon: String? = null,
    val variant: ChipVariant = ChipVariant.ASSIST,
    val stateKey: String? = null,
    val value: String? = null,
    val isEnabled: Boolean = true,
    val actions: List<HeimAction> = emptyList()
) : HeimComponent

/** Compact pill-shaped label, for statuses, counts and tags. */
public data class BadgeComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val text: String,
    val backgroundColor: String = "primaryContainer",
    val textColor: String = "onPrimaryContainer",
    val iconUrl: String? = null
) : HeimComponent

/**
 * Tappable button that dispatches [actions] when pressed.
 *
 * @property isLoading replaces the label with a spinner and blocks interaction.
 * @property icon optional leading icon, named rather than drawn — the host's
 *   [io.heimui.core.presentation.designsystem.HeimIconProvider] decides what it looks like. Prefer
 *   this to an emoji in [title]: an emoji is read aloud by screen readers, renders differently on
 *   every OS version, and cannot be tinted with the button's content colour.
 */
public data class ButtonComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val title: String,
    val variant: ButtonVariant = ButtonVariant.FILLED,
    val isFullWidth: Boolean = false,
    val isEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val icon: String? = null,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val stateKey: String,
    val label: String? = null,
    val placeholder: String? = null,
    val inputType: InputType = InputType.TEXT,
    val initialValue: String = "",
    val validationRules: List<ValidationRule> = emptyList(),
    val helperText: String? = null
) : HeimComponent

/**
 * One choice in a [RadioGroupComponent] or a [SelectComponent].
 *
 * [value] is stored and submitted; [label] is read. Separate so the label can be translated
 * without the stored value moving underneath the backend.
 */
public data class HeimOption(
    val value: String,
    val label: String
)

/** Single on/off choice bound to [stateKey]. Unlike a switch, it reads as "I agree", not "on". */
public data class CheckboxComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val stateKey: String,
    val label: String,
    val initialChecked: Boolean = false,
    val validationRules: List<ValidationRule> = emptyList(),
    val onCheckActions: List<HeimAction> = emptyList()
) : HeimComponent

/** One of [options], all visible at once. Use it under about five choices; above that, a select. */
public data class RadioGroupComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val stateKey: String,
    val label: String? = null,
    val options: List<HeimOption> = emptyList(),
    val initialValue: String = "",
    val validationRules: List<ValidationRule> = emptyList(),
    val onSelectActions: List<HeimAction> = emptyList()
) : HeimComponent

/** One of [options], revealed on demand. The right shape for a country or document-type list. */
public data class SelectComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val stateKey: String,
    val label: String? = null,
    val placeholder: String? = null,
    val options: List<HeimOption> = emptyList(),
    val initialValue: String = "",
    val validationRules: List<ValidationRule> = emptyList(),
    val onSelectActions: List<HeimAction> = emptyList()
) : HeimComponent

/**
 * Date input. State holds ISO-8601 `YYYY-MM-DD`; the field shows the user's own format.
 *
 * @property minDate earliest selectable date, ISO. Null leaves it open.
 * @property maxDate latest selectable date, ISO. Null leaves it open.
 */
public data class DatePickerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val stateKey: String,
    val label: String? = null,
    val placeholder: String? = null,
    val initialValue: String = "",
    val minDate: String? = null,
    val maxDate: String? = null,
    val validationRules: List<ValidationRule> = emptyList(),
    val onSelectActions: List<HeimAction> = emptyList()
) : HeimComponent

/** Boolean toggle bound to [stateKey], dispatching [onCheckActions] on every change. */
public data class SwitchComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val size: Int,
    val isFlexible: Boolean = false
) : HeimComponent

/** Horizontal rule separating sections. */
public data class DividerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
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
    override val weight: Float? = null,
    override val frame: HeimSize = HeimSize.None,
    val originalType: String? = null
) : HeimComponent
