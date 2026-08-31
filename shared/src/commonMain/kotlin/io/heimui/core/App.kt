package io.heimui.core

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.accessibility.AccessibilityRole
import io.heimui.core.domain.model.accessibility.HeimAccessibility
import io.heimui.core.domain.model.action.NavigateAction
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.component.*
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import io.heimui.core.presentation.HeimScreenRenderer
import io.heimui.core.presentation.designsystem.HeimTheme
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun App() {
    HeimTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val stateManager = remember { HeimStateManager(screenId = "demo_onboarding") }

        // JSON Payload simulating Server-Driven response from HeimUI Server
        val demoScreen = remember {
            val originalScreen = HeimScreenResponse(
                id = "onboarding_showcase",
                version = "1.7.0",
                title = "HeimUI Showcase",
                applySafeInsets = true,
                root = ContainerComponent(
                    id = "root_column",
                    direction = Direction.VERTICAL,
                    padding = 20,
                    spacing = 16,
                    children = listOf(
                        // Header Row with Badge and Icon
                        ContainerComponent(
                            id = "header_row",
                            direction = Direction.HORIZONTAL,
                            alignment = Alignment.CENTER,
                            spacing = 8,
                            children = listOf(
                                IconComponent(
                                    id = "brand_icon",
                                    name = "star",
                                    tint = "primary",
                                    size = 28
                                ),
                                TextComponent(
                                    id = "app_title",
                                    text = "HeimUI Showcase",
                                    style = "titleLarge",
                                    color = "primary",
                                    a11y = HeimAccessibility(
                                        contentDescription = "HeimUI Showcase Header",
                                        role = AccessibilityRole.HEADER,
                                        isHeading = true
                                    )
                                ),
                                SpacerComponent(id = "header_spacer", size = 0, isFlexible = true),
                                BadgeComponent(
                                    id = "version_badge",
                                    text = "v0.0.1-alpha",
                                    backgroundColor = "primaryContainer",
                                    textColor = "onPrimaryContainer"
                                )
                            )
                        ),
                        TextComponent(
                            id = "subtitle",
                            text = "Server-Driven UI running natively on Kotlin Multiplatform & Compose.",
                            style = "bodyMedium",
                            color = "onSurfaceVariant"
                        ),
                        DividerComponent(id = "divider_1", thickness = 1, color = "outlineVariant"),
                        // Form Section
                        TextComponent(
                            id = "form_title",
                            text = "Registro de Usuario",
                            style = "titleMedium"
                        ),
                        TextFieldComponent(
                            id = "input_name",
                            stateKey = "user_name",
                            label = "Nombre Completo",
                            placeholder = "Ingresa tu nombre",
                            inputType = InputType.TEXT,
                            validationRules = listOf(
                                ValidationRule(
                                    type = ValidationType.REQUIRED,
                                    errorMessage = "El nombre es obligatorio"
                                ),
                                ValidationRule(
                                    type = ValidationType.MIN_LENGTH,
                                    value = "3",
                                    errorMessage = "Debe tener al menos 3 caracteres"
                                )
                            )
                        ),
                        TextFieldComponent(
                            id = "input_email",
                            stateKey = "user_email",
                            label = "Correo Electrónico",
                            placeholder = "ejemplo@empresa.com",
                            inputType = InputType.EMAIL,
                            validationRules = listOf(
                                ValidationRule(
                                    type = ValidationType.REQUIRED,
                                    errorMessage = "El correo es obligatorio"
                                ),
                                ValidationRule(
                                    type = ValidationType.EMAIL,
                                    errorMessage = "Formato de correo inválido"
                                )
                            )
                        ),
                        // Enterprise Switch
                        SwitchComponent(
                            id = "switch_enterprise",
                            stateKey = "is_enterprise",
                            label = "¿Es una cuenta empresarial?",
                            initialChecked = false
                        ),
                        // Conditional field visible only if enterprise is true
                        TextFieldComponent(
                            id = "input_company",
                            visibleIf = "is_enterprise == 'true'",
                            stateKey = "company_name",
                            label = "Nombre de la Empresa",
                            placeholder = "Acme Inc.",
                            inputType = InputType.TEXT,
                            validationRules = listOf(
                                ValidationRule(
                                    type = ValidationType.REQUIRED,
                                    errorMessage = "La empresa es obligatoria para cuentas corporativas"
                                )
                            )
                        ),
                        // Feature Card
                        CardComponent(
                            id = "info_card",
                            elevation = 2,
                            cornerRadius = 12,
                            backgroundColor = "surfaceVariant",
                            padding = 12,
                            actions = listOf(
                                ShowSnackbarAction(message = "¡Card presionado en Server-Driven UI!")
                            ),
                            child = ContainerComponent(
                                id = "card_inner",
                                direction = Direction.HORIZONTAL,
                                spacing = 12,
                                alignment = Alignment.CENTER,
                                children = listOf(
                                    IconComponent(id = "card_icon", name = "favorite", tint = "primary", size = 24),
                                    TextComponent(
                                        id = "card_text",
                                        text = "Toca esta tarjeta para disparar una acción remota.",
                                        style = "bodySmall"
                                    )
                                )
                            )
                        ),
                        SpacerComponent(id = "bottom_spacer", size = 8),
                        // Submit Button
                        ButtonComponent(
                            id = "btn_submit",
                            title = "Enviar Solicitud",
                            variant = ButtonVariant.FILLED,
                            isFullWidth = true,
                            actions = listOf(
                                SubmitFormAction(endpoint = "/api/v1/onboarding")
                            )
                        )
                    )
                )
            )

            // Simulate full JSON Roundtrip Serialization
            val jsonString = Json.encodeToString(originalScreen)
            Json.decodeFromString<HeimScreenResponse>(jsonString)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            HeimScreenRenderer(
                response = demoScreen,
                stateManager = stateManager,
                onAction = { action ->
                    when (action) {
                        is ShowSnackbarAction -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(action.message)
                            }
                        }
                        is SubmitFormAction -> {
                            val name = stateManager.getValue("user_name").ifBlank { "(sin nombre)" }
                            val email = stateManager.getValue("user_email").ifBlank { "(sin email)" }
                            scope.launch {
                                snackbarHostState.showSnackbar("Enviado a ${action.endpoint} - Usuario: $name, Email: $email")
                            }
                        }
                        is NavigateAction -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("Navegando a: ${action.screenId}")
                            }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}
