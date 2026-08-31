package io.heimui.core.domain.model.component

import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.validation.ValidationRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface HeimComponent {
    val id: String
    val visibleIf: String? get() = null
    val a11y: HeimAccessibility? get() = null
}

@Serializable
enum class Direction {
    @SerialName("VERTICAL") VERTICAL,
    @SerialName("HORIZONTAL") HORIZONTAL
}

@Serializable
enum class Alignment {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("TOP") TOP,
    @SerialName("BOTTOM") BOTTOM
}

@Serializable
enum class TextAlign {
    @SerialName("START") START,
    @SerialName("CENTER") CENTER,
    @SerialName("END") END,
    @SerialName("JUSTIFY") JUSTIFY
}

@Serializable
enum class ContentScale {
    @SerialName("CROP") CROP,
    @SerialName("FIT") FIT,
    @SerialName("FILL_BOUNDS") FILL_BOUNDS,
    @SerialName("INSIDE") INSIDE
}

@Serializable
enum class ButtonVariant {
    @SerialName("FILLED") FILLED,
    @SerialName("OUTLINED") OUTLINED,
    @SerialName("TEXT") TEXT,
    @SerialName("TONAL") TONAL
}

@Serializable
enum class InputType {
    @SerialName("TEXT") TEXT,
    @SerialName("NUMBER") NUMBER,
    @SerialName("EMAIL") EMAIL,
    @SerialName("PASSWORD") PASSWORD,
    @SerialName("PHONE") PHONE
}

// 1. Container (Flexbox Column / Row)
@Serializable
@SerialName("container")
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

// 2. Box (Superposición / Z-Index)
@Serializable
@SerialName("box")
data class BoxComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val contentAlignment: Alignment = Alignment.CENTER,
    val children: List<HeimComponent> = emptyList()
) : HeimComponent

// 3. LazyColumn (Lista Vertical Paginada)
@Serializable
@SerialName("lazy_column")
data class LazyColumnComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

// 4. LazyRow (Carrusel Horizontal Paginado)
@Serializable
@SerialName("lazy_row")
data class LazyRowComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val spacing: Int = 8,
    val padding: Int = 0,
    val items: List<HeimComponent> = emptyList(),
    val pagination: PaginationConfig? = null
) : HeimComponent

@Serializable
data class PaginationConfig(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loadThreshold: Int = 3,
    val onLoadMoreActions: List<HeimAction> = emptyList()
)

// 5. Text
@Serializable
@SerialName("text")
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
@Serializable
@SerialName("image")
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
@Serializable
@SerialName("card")
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
@Serializable
@SerialName("badge")
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
@Serializable
@SerialName("button")
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

// 10. TextField (Data Binding + Validaciones + A11y)
@Serializable
@SerialName("text_field")
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
@Serializable
@SerialName("switch")
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
@Serializable
@SerialName("icon")
data class IconComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val tint: String? = null,
    val size: Int = 24
) : HeimComponent

// 13. Spacer
@Serializable
@SerialName("spacer")
data class SpacerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val size: Int,
    val isFlexible: Boolean = false
) : HeimComponent

// 14. Divider
@Serializable
@SerialName("divider")
data class DividerComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val thickness: Int = 1,
    val color: String = "outlineVariant"
) : HeimComponent

// 15. Custom & Unknown (Escape Hatch y Fallback)
@Serializable
@SerialName("custom")
data class CustomComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null,
    val name: String,
    val data: JsonObject
) : HeimComponent

@Serializable
@SerialName("unknown")
data class UnknownComponent(
    override val id: String,
    override val visibleIf: String? = null,
    override val a11y: HeimAccessibility? = null
) : HeimComponent
