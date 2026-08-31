package io.heimui.core.domain.model.component

import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.validation.ValidationRule

sealed interface HeimComponent {
    val id: String
    val visibleIf: String? get() = null
    val a11y: HeimAccessibility? get() = null
}

enum class Direction {
    VERTICAL,
    HORIZONTAL
}

enum class Alignment {
    START,
    CENTER,
    END,
    TOP,
    BOTTOM
}

enum class TextAlign {
    START,
    CENTER,
    END,
    JUSTIFY
}

enum class ContentScale {
    CROP,
    FIT,
    FILL_BOUNDS,
    INSIDE
}

enum class ButtonVariant {
    FILLED,
    OUTLINED,
    TEXT,
    TONAL
}

enum class InputType {
    TEXT,
    NUMBER,
    EMAIL,
    PASSWORD,
    PHONE
}

// 1. Container (Flexbox Column / Row)
data class ContainerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val direction: Direction = Direction.VERTICAL,
    val alignment: Alignment = Alignment.START,
    val padding: Int = 0,
    val spacing: Int = 0,
    val backgroundColor: String? = null,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

// 2. Box (Z-Index / Overlay)
data class BoxComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val contentAlignment: Alignment = Alignment.CENTER,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

// 3. LazyColumn (Paginated Vertical List)
data class LazyColumnComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

// 4. LazyRow (Paginated Horizontal Carousel)
data class LazyRowComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

data class PaginationConfig(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadThreshold: Int = 3,
    val onLoadMoreActions: List<HeimAction> = emptyList()
)

// 5. Text
data class TextComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val text: String,
    val style: String = "bodyMedium",
    val color: String? = null,
    val maxLines: Int? = null,
    val textAlign: TextAlign = TextAlign.START
) : HeimComponent

// 6. Image (Coil 3 Async + BlurHash)
data class ImageComponent(
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

// 7. Card
data class CardComponent(
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

// 8. Badge / Chip
data class BadgeComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val text: String,
    val backgroundColor: String = "primaryContainer",
    val textColor: String = "onPrimaryContainer",
    val iconUrl: String? = null
) : HeimComponent

// 9. Button
data class ButtonComponent(
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

// 10. TextField (Data Binding + Validations + A11y)
data class TextFieldComponent(
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

// 11. Switch
data class SwitchComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val stateKey: String,
    val label: String,
    val initialChecked: Boolean = false,
    val onCheckActions: List<HeimAction> = emptyList()
) : HeimComponent

// 12. Icon
data class IconComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val tint: String? = null,
    val size: Int = 24
) : HeimComponent

// 13. Spacer
data class SpacerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val size: Int,
    val isFlexible: Boolean = false
) : HeimComponent

// 14. Divider
data class DividerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val thickness: Int = 1,
    val color: String = "outlineVariant"
) : HeimComponent

// 15. Custom & Unknown (Escape Hatch and Fallback)
data class CustomComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val data: Map<String, Any?> = emptyMap()
) : HeimComponent

data class UnknownComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null
) : HeimComponent
