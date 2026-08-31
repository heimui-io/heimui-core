package io.heimui.core.data.mapper

import io.heimui.core.data.dto.*
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.*
import io.heimui.core.domain.model.component.*
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import kotlinx.serialization.json.*

fun HeimScreenResponseDto.toDomain(): HeimScreenResponse {
    return HeimScreenResponse(
        id = id,
        version = version,
        title = title,
        applySafeInsets = applySafeInsets,
        root = root.toDomain(),
        metadata = metadata?.toMapValue(),
        signature = signature
    )
}

fun HeimComponentDto.toDomain(): HeimComponent {
    return when (this) {
        is ContainerComponentDto -> ContainerComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            direction = when (direction) {
                DirectionDto.VERTICAL -> Direction.VERTICAL
                DirectionDto.HORIZONTAL -> Direction.HORIZONTAL
            },
            alignment = when (alignment) {
                AlignmentDto.START -> Alignment.START
                AlignmentDto.CENTER -> Alignment.CENTER
                AlignmentDto.END -> Alignment.END
                AlignmentDto.TOP -> Alignment.TOP
                AlignmentDto.BOTTOM -> Alignment.BOTTOM
            },
            padding = padding,
            spacing = spacing,
            backgroundColor = backgroundColor,
            children = children.map { it.toDomain() }
        )
        is BoxComponentDto -> BoxComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            contentAlignment = when (contentAlignment) {
                AlignmentDto.START -> Alignment.START
                AlignmentDto.CENTER -> Alignment.CENTER
                AlignmentDto.END -> Alignment.END
                AlignmentDto.TOP -> Alignment.TOP
                AlignmentDto.BOTTOM -> Alignment.BOTTOM
            },
            children = children.map { it.toDomain() }
        )
        is LazyColumnComponentDto -> LazyColumnComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            spacing = spacing,
            padding = padding,
            items = items.map { it.toDomain() },
            pagination = pagination?.toDomain()
        )
        is LazyRowComponentDto -> LazyRowComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            spacing = spacing,
            padding = padding,
            items = items.map { it.toDomain() },
            pagination = pagination?.toDomain()
        )
        is TextComponentDto -> TextComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            textAlign = when (textAlign) {
                TextAlignDto.START -> TextAlign.START
                TextAlignDto.CENTER -> TextAlign.CENTER
                TextAlignDto.END -> TextAlign.END
                TextAlignDto.JUSTIFY -> TextAlign.JUSTIFY
            }
        )
        is ImageComponentDto -> ImageComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            url = url,
            blurHash = blurHash,
            aspectRatio = aspectRatio,
            height = height,
            cornerRadius = cornerRadius,
            contentScale = when (contentScale) {
                ContentScaleDto.CROP -> ContentScale.CROP
                ContentScaleDto.FIT -> ContentScale.FIT
                ContentScaleDto.FILL_BOUNDS -> ContentScale.FILL_BOUNDS
                ContentScaleDto.INSIDE -> ContentScale.INSIDE
            }
        )
        is CardComponentDto -> CardComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            elevation = elevation,
            cornerRadius = cornerRadius,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            padding = padding,
            actions = actions.map { it.toDomain() },
            child = child.toDomain()
        )
        is BadgeComponentDto -> BadgeComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            text = text,
            backgroundColor = backgroundColor,
            textColor = textColor,
            iconUrl = iconUrl
        )
        is ButtonComponentDto -> ButtonComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            title = title,
            variant = when (variant) {
                ButtonVariantDto.FILLED -> ButtonVariant.FILLED
                ButtonVariantDto.OUTLINED -> ButtonVariant.OUTLINED
                ButtonVariantDto.TEXT -> ButtonVariant.TEXT
                ButtonVariantDto.TONAL -> ButtonVariant.TONAL
            },
            isFullWidth = isFullWidth,
            isEnabled = isEnabled,
            isLoading = isLoading,
            actions = actions.map { it.toDomain() }
        )
        is TextFieldComponentDto -> TextFieldComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            stateKey = stateKey,
            label = label,
            placeholder = placeholder,
            inputType = when (inputType) {
                InputTypeDto.TEXT -> InputType.TEXT
                InputTypeDto.NUMBER -> InputType.NUMBER
                InputTypeDto.EMAIL -> InputType.EMAIL
                InputTypeDto.PASSWORD -> InputType.PASSWORD
                InputTypeDto.PHONE -> InputType.PHONE
            },
            initialValue = initialValue,
            validationRules = validationRules.map { it.toDomain() },
            helperText = helperText
        )
        is SwitchComponentDto -> SwitchComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            stateKey = stateKey,
            label = label,
            initialChecked = initialChecked,
            onCheckActions = onCheckActions.map { it.toDomain() }
        )
        is IconComponentDto -> IconComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            name = name,
            tint = tint,
            size = size
        )
        is SpacerComponentDto -> SpacerComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            size = size,
            isFlexible = isFlexible
        )
        is DividerComponentDto -> DividerComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            thickness = thickness,
            color = color
        )
        is CustomComponentDto -> CustomComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            name = name,
            data = data?.toMapValue() ?: emptyMap()
        )
        is UnknownComponentDto -> UnknownComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain()
        )
    }
}

fun PaginationConfigDto.toDomain() = PaginationConfig(
    nextCursor = nextCursor,
    hasMore = hasMore,
    loadThreshold = loadThreshold,
    onLoadMoreActions = onLoadMoreActions.map { it.toDomain() }
)

fun HeimActionDto.toDomain(): HeimAction {
    return when (this) {
        is NavigateActionDto -> NavigateAction(screenId = screenId, params = params)
        is SubmitFormActionDto -> SubmitFormAction(endpoint = endpoint, method = method, payload = payload?.toMapValue())
        is ShowSnackbarActionDto -> ShowSnackbarAction(message = message, duration = duration)
        is OpenUrlActionDto -> OpenUrlAction(url = url)
        is CustomActionDto -> CustomAction(name = name, payload = payload?.toMapValue())
        is ShowBottomSheetActionDto -> ShowBottomSheetAction(
            title = title,
            isDismissible = isDismissible,
            content = content.toDomain()
        )
        is ShowDialogActionDto -> ShowDialogAction(
            title = title,
            message = message,
            confirmText = confirmText,
            confirmActions = confirmActions.map { it.toDomain() },
            dismissText = dismissText,
            dismissActions = dismissActions.map { it.toDomain() }
        )
        is DismissModalActionDto -> DismissModalAction
        is DismissActionDto -> DismissAction
    }
}

fun HeimAccessibilityDto.toDomain() = HeimAccessibility(
    contentDescription = contentDescription,
    role = role?.let {
        when (it) {
            AccessibilityRoleDto.BUTTON -> AccessibilityRole.BUTTON
            AccessibilityRoleDto.IMAGE -> AccessibilityRole.IMAGE
            AccessibilityRoleDto.HEADER -> AccessibilityRole.HEADER
            AccessibilityRoleDto.SWITCH -> AccessibilityRole.SWITCH
            AccessibilityRoleDto.TAB -> AccessibilityRole.TAB
        }
    },
    isHeading = isHeading,
    stateDescription = stateDescription,
    hiddenFromAccessibility = hiddenFromAccessibility
)

fun ValidationRuleDto.toDomain() = ValidationRule(
    type = when (type) {
        ValidationTypeDto.REQUIRED -> ValidationType.REQUIRED
        ValidationTypeDto.REGEX -> ValidationType.REGEX
        ValidationTypeDto.MIN_LENGTH -> ValidationType.MIN_LENGTH
        ValidationTypeDto.MAX_LENGTH -> ValidationType.MAX_LENGTH
        ValidationTypeDto.EMAIL -> ValidationType.EMAIL
        ValidationTypeDto.NUMERIC -> ValidationType.NUMERIC
        ValidationTypeDto.CUSTOM -> ValidationType.CUSTOM
    },
    value = value,
    errorMessage = errorMessage
)

fun JsonObject.toMapValue(): Map<String, Any?> {
    return this.mapValues { (_, value) -> value.toPrimitiveValue() }
}

fun JsonElement.toPrimitiveValue(): Any? {
    return when (this) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (isString) content
            else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        }
        is JsonArray -> this.map { it.toPrimitiveValue() }
        is JsonObject -> this.toMapValue()
    }
}
