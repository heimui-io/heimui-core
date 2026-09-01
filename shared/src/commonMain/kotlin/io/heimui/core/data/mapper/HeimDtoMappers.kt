package io.heimui.core.data.mapper

import io.heimui.core.data.dto.AccessibilityRoleDto
import io.heimui.core.data.dto.AlignmentDto
import io.heimui.core.data.dto.BadgeComponentDto
import io.heimui.core.data.dto.BoxComponentDto
import io.heimui.core.data.dto.ButtonComponentDto
import io.heimui.core.data.dto.ButtonVariantDto
import io.heimui.core.data.dto.CardComponentDto
import io.heimui.core.data.dto.ContainerComponentDto
import io.heimui.core.data.dto.ContentScaleDto
import io.heimui.core.data.dto.CustomActionDto
import io.heimui.core.data.dto.CustomComponentDto
import io.heimui.core.data.dto.DirectionDto
import io.heimui.core.data.dto.DismissActionDto
import io.heimui.core.data.dto.DismissModalActionDto
import io.heimui.core.data.dto.DividerComponentDto
import io.heimui.core.data.dto.HeimAccessibilityDto
import io.heimui.core.data.dto.HeimActionDto
import io.heimui.core.data.dto.HeimComponentDto
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.dto.IconComponentDto
import io.heimui.core.data.dto.ImageComponentDto
import io.heimui.core.data.dto.InputTypeDto
import io.heimui.core.data.dto.LazyColumnComponentDto
import io.heimui.core.data.dto.LazyRowComponentDto
import io.heimui.core.data.dto.NavigateActionDto
import io.heimui.core.data.dto.OpenUrlActionDto
import io.heimui.core.data.dto.PaginationConfigDto
import io.heimui.core.data.dto.ShowBottomSheetActionDto
import io.heimui.core.data.dto.ShowDialogActionDto
import io.heimui.core.data.dto.ShowSnackbarActionDto
import io.heimui.core.data.dto.SpacerComponentDto
import io.heimui.core.data.dto.SubmitFormActionDto
import io.heimui.core.data.dto.SwitchComponentDto
import io.heimui.core.data.dto.TextAlignDto
import io.heimui.core.data.dto.TextComponentDto
import io.heimui.core.data.dto.TextFieldComponentDto
import io.heimui.core.data.dto.UnknownActionDto
import io.heimui.core.data.dto.UnknownComponentDto
import io.heimui.core.data.dto.ValidationRuleDto
import io.heimui.core.data.dto.ValidationTypeDto
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.CustomAction
import io.heimui.core.domain.model.action.DismissAction
import io.heimui.core.domain.model.action.DismissModalAction
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.NavigateAction
import io.heimui.core.domain.model.action.OpenUrlAction
import io.heimui.core.domain.model.action.ShowBottomSheetAction
import io.heimui.core.domain.model.action.ShowDialogAction
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.action.UnknownAction
import io.heimui.core.domain.model.component.Alignment
import io.heimui.core.domain.model.component.BadgeComponent
import io.heimui.core.domain.model.component.BoxComponent
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.component.ButtonVariant
import io.heimui.core.domain.model.component.CardComponent
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.ContentScale
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.domain.model.component.Direction
import io.heimui.core.domain.model.component.DividerComponent
import io.heimui.core.domain.model.component.HeimComponent
import io.heimui.core.domain.model.component.IconComponent
import io.heimui.core.domain.model.component.ImageComponent
import io.heimui.core.domain.model.component.InputType
import io.heimui.core.domain.model.component.LazyColumnComponent
import io.heimui.core.domain.model.component.LazyRowComponent
import io.heimui.core.domain.model.component.PaginationConfig
import io.heimui.core.domain.model.component.SpacerComponent
import io.heimui.core.domain.model.component.SwitchComponent
import io.heimui.core.domain.model.component.TextAlign
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.domain.model.component.TextFieldComponent
import io.heimui.core.domain.model.component.UnknownComponent
import io.heimui.core.domain.model.toHeimValue
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import kotlinx.serialization.json.JsonObject

internal const val MAX_COMPONENT_DEPTH = 64

/** Collects the repairs performed while mapping one payload. */
internal class HeimMappingReport {
    private val entries = mutableListOf<String>()
    val violations: List<String> get() = entries
    fun record(message: String) {
        if (entries.size < MAX_RECORDED_VIOLATIONS) entries += message
    }
    private companion object { const val MAX_RECORDED_VIOLATIONS = 50 }
}

internal fun HeimScreenResponseDto.toDomain(): HeimScreenResponse {
    val report = HeimMappingReport()
    return HeimScreenResponse(
        id = id,
        version = version,
        title = title,
        applySafeInsets = applySafeInsets,
        root = root.toDomain(depth = 0, report = report),
        metadata = metadata?.toMapHeimValue(),
        signature = signature,
        violations = report.violations
    )
}

internal fun HeimComponentDto.toDomain(
    depth: Int = 0,
    report: HeimMappingReport = HeimMappingReport()
): HeimComponent {
    if (depth > MAX_COMPONENT_DEPTH) {
        report.record("Tree pruned at id='$id': exceeded max depth $MAX_COMPONENT_DEPTH")
        return UnknownComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            originalType = "depth_limit_exceeded"
        )
    }

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
            padding = maxOf(0, padding),
            spacing = maxOf(0, spacing),
            backgroundColor = backgroundColor,
            scrollable = scrollable,
            children = children.mapDeduplicated(depth = depth + 1, report = report)
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
            children = children.mapDeduplicated(depth = depth + 1, report = report)
        )
        is LazyColumnComponentDto -> LazyColumnComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            spacing = maxOf(0, spacing),
            padding = maxOf(0, padding),
            items = items.mapDeduplicated(depth = depth + 1, report = report),
            pagination = pagination?.toDomain()
        )
        is LazyRowComponentDto -> LazyRowComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            spacing = maxOf(0, spacing),
            padding = maxOf(0, padding),
            items = items.mapDeduplicated(depth = depth + 1, report = report),
            pagination = pagination?.toDomain()
        )
        is TextComponentDto -> TextComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            text = text,
            style = style,
            color = color,
            maxLines = maxLines?.let { maxOf(1, it) },
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
            aspectRatio = if (aspectRatio != null && aspectRatio > 0f) aspectRatio else null,
            height = height?.let { maxOf(1, it) },
            cornerRadius = maxOf(0, cornerRadius),
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
            elevation = maxOf(0, elevation),
            cornerRadius = maxOf(0, cornerRadius),
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            padding = maxOf(0, padding),
            actions = actions.map { it.toDomain() },
            child = child.toDomain(depth = depth + 1)
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
            size = maxOf(1, size)
        )
        is SpacerComponentDto -> SpacerComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            size = maxOf(0, size),
            isFlexible = isFlexible
        )
        is DividerComponentDto -> DividerComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            thickness = maxOf(1, thickness),
            color = color
        )
        is CustomComponentDto -> CustomComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            name = name,
            data = data?.toMapHeimValue() ?: emptyMap()
        )
        is UnknownComponentDto -> UnknownComponent(
            id = id,
            visibleIf = visibleIf,
            a11y = a11y?.toDomain(),
            originalType = originalType
        )
    }
}

private fun List<HeimComponentDto>.mapDeduplicated(
    depth: Int,
    report: HeimMappingReport = HeimMappingReport()
): List<HeimComponent> {
    val seenIds = mutableSetOf<String>()
    return mapIndexed { index, childDto ->
        val mapped = childDto.toDomain(depth = depth, report = report)
        // A blank id is a valid LazyColumn key but makes every sibling collide; synthesise one.
        val child = if (mapped.id.isBlank()) mapped.withId("heim_node_$index") else mapped
        if (seenIds.add(child.id)) {
            child
        } else {
            report.record("Duplicate component id '${child.id}' disambiguated to '${child.id}_$index'")
            // Deduplicate conflicting ID
            when (child) {
                is TextComponent -> child.copy(id = "${child.id}_$index")
                is ButtonComponent -> child.copy(id = "${child.id}_$index")
                is ImageComponent -> child.copy(id = "${child.id}_$index")
                is CardComponent -> child.copy(id = "${child.id}_$index")
                is ContainerComponent -> child.copy(id = "${child.id}_$index")
                is BoxComponent -> child.copy(id = "${child.id}_$index")
                is TextFieldComponent -> child.copy(id = "${child.id}_$index")
                is SwitchComponent -> child.copy(id = "${child.id}_$index")
                is BadgeComponent -> child.copy(id = "${child.id}_$index")
                is IconComponent -> child.copy(id = "${child.id}_$index")
                is SpacerComponent -> child.copy(id = "${child.id}_$index")
                is DividerComponent -> child.copy(id = "${child.id}_$index")
                is LazyColumnComponent -> child.copy(id = "${child.id}_$index")
                is LazyRowComponent -> child.copy(id = "${child.id}_$index")
                is CustomComponent -> child.copy(id = "${child.id}_$index")
                is UnknownComponent -> child.copy(id = "${child.id}_$index")
            }
        }
    }
}

private fun HeimComponent.withId(newId: String): HeimComponent = when (this) {
    is TextComponent -> copy(id = newId)
    is ButtonComponent -> copy(id = newId)
    is ImageComponent -> copy(id = newId)
    is CardComponent -> copy(id = newId)
    is ContainerComponent -> copy(id = newId)
    is BoxComponent -> copy(id = newId)
    is TextFieldComponent -> copy(id = newId)
    is SwitchComponent -> copy(id = newId)
    is BadgeComponent -> copy(id = newId)
    is IconComponent -> copy(id = newId)
    is SpacerComponent -> copy(id = newId)
    is DividerComponent -> copy(id = newId)
    is LazyColumnComponent -> copy(id = newId)
    is LazyRowComponent -> copy(id = newId)
    is CustomComponent -> copy(id = newId)
    is UnknownComponent -> copy(id = newId)
}

internal fun PaginationConfigDto.toDomain() = PaginationConfig(
    nextCursor = nextCursor,
    hasMore = hasMore,
    loadThreshold = loadThreshold,
    onLoadMoreActions = onLoadMoreActions.map { it.toDomain() }
)

internal fun HeimActionDto.toDomain(): HeimAction {
    return when (this) {
        is NavigateActionDto -> NavigateAction(screenId = screenId, params = params)
        is SubmitFormActionDto -> SubmitFormAction(endpoint = endpoint, method = method, payload = payload?.toMapHeimValue())
        is ShowSnackbarActionDto -> ShowSnackbarAction(message = message, duration = duration)
        is OpenUrlActionDto -> OpenUrlAction(url = url)
        is CustomActionDto -> CustomAction(name = name, payload = payload?.toMapHeimValue())
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
        is UnknownActionDto -> UnknownAction(originalType = originalType)
    }
}

internal fun HeimAccessibilityDto.toDomain() = HeimAccessibility(
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

internal fun ValidationRuleDto.toDomain() = ValidationRule(
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

internal fun JsonObject.toMapHeimValue(): Map<String, HeimValue> {
    return this.mapValues { (_, value) -> value.toHeimValue() }
}
