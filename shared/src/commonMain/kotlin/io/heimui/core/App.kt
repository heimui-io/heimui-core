package io.heimui.core

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.heimui.core.data.dto.HeimScreenResponseDto
import io.heimui.core.data.mapper.toDomain
import io.heimui.core.domain.model.action.NavigateAction
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.presentation.HeimScreenRenderer
import io.heimui.core.presentation.designsystem.HeimTheme
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun App() {
    HeimTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val stateManager = remember { HeimStateManager(screenId = "demo_onboarding") }

        // Raw Server-Driven UI JSON payload received from remote backend / cache
        val demoScreen = remember {
            val rawJson = """
            {
                "id": "onboarding_showcase",
                "version": "1.7.0",
                "title": "HeimUI Showcase",
                "apply_safe_insets": true,
                "root": {
                    "type": "container",
                    "id": "root_column",
                    "direction": "VERTICAL",
                    "padding": 20,
                    "spacing": 16,
                    "children": [
                        {
                            "type": "container",
                            "id": "header_row",
                            "direction": "HORIZONTAL",
                            "alignment": "CENTER",
                            "spacing": 8,
                            "children": [
                                {
                                    "type": "icon",
                                    "id": "brand_icon",
                                    "name": "star",
                                    "tint": "primary",
                                    "size": 28
                                },
                                {
                                    "type": "text",
                                    "id": "app_title",
                                    "text": "HeimUI Showcase",
                                    "style": "titleLarge",
                                    "color": "primary",
                                    "a11y": {
                                        "content_description": "HeimUI Showcase Header",
                                        "role": "HEADER",
                                        "is_heading": true
                                    }
                                },
                                {
                                    "type": "spacer",
                                    "id": "header_spacer",
                                    "size": 0,
                                    "is_flexible": true
                                },
                                {
                                    "type": "badge",
                                    "id": "version_badge",
                                    "text": "v0.0.1-alpha",
                                    "background_color": "primaryContainer",
                                    "text_color": "onPrimaryContainer"
                                }
                            ]
                        },
                        {
                            "type": "text",
                            "id": "subtitle",
                            "text": "Server-Driven UI running natively on Kotlin Multiplatform & Compose.",
                            "style": "bodyMedium",
                            "color": "onSurfaceVariant"
                        },
                        {
                            "type": "divider",
                            "id": "divider_1",
                            "thickness": 1,
                            "color": "outlineVariant"
                        },
                        {
                            "type": "text",
                            "id": "form_title",
                            "text": "User Registration",
                            "style": "titleMedium"
                        },
                        {
                            "type": "text_field",
                            "id": "input_name",
                            "state_key": "user_name",
                            "label": "Full Name",
                            "placeholder": "Enter your name",
                            "input_type": "TEXT",
                            "validation_rules": [
                                {
                                    "type": "REQUIRED",
                                    "error_message": "Name is required"
                                },
                                {
                                    "type": "MIN_LENGTH",
                                    "value": "3",
                                    "error_message": "Must be at least 3 characters"
                                }
                            ]
                        },
                        {
                            "type": "text_field",
                            "id": "input_email",
                            "state_key": "user_email",
                            "label": "Email Address",
                            "placeholder": "user@company.com",
                            "input_type": "EMAIL",
                            "validation_rules": [
                                {
                                    "type": "REQUIRED",
                                    "error_message": "Email is required"
                                },
                                {
                                    "type": "EMAIL",
                                    "error_message": "Invalid email format"
                                }
                            ]
                        },
                        {
                            "type": "switch",
                            "id": "switch_enterprise",
                            "state_key": "is_enterprise",
                            "label": "Is this an enterprise account?",
                            "initial_checked": false
                        },
                        {
                            "type": "text_field",
                            "id": "input_company",
                            "visible_if": "is_enterprise == 'true'",
                            "state_key": "company_name",
                            "label": "Company Name",
                            "placeholder": "Acme Inc.",
                            "input_type": "TEXT",
                            "validation_rules": [
                                {
                                    "type": "REQUIRED",
                                    "error_message": "Company name is required for enterprise accounts"
                                }
                            ]
                        },
                        {
                            "type": "card",
                            "id": "info_card",
                            "elevation": 2,
                            "corner_radius": 12,
                            "background_color": "surfaceVariant",
                            "padding": 12,
                            "actions": [
                                {
                                    "type": "show_snackbar",
                                    "message": "Card clicked via Server-Driven UI action!"
                                }
                            ],
                            "child": {
                                "type": "container",
                                "id": "card_inner",
                                "direction": "HORIZONTAL",
                                "spacing": 12,
                                "alignment": "CENTER",
                                "children": [
                                    {
                                        "type": "icon",
                                        "id": "card_icon",
                                        "name": "favorite",
                                        "tint": "primary",
                                        "size": 24
                                    },
                                    {
                                        "type": "text",
                                        "id": "card_text",
                                        "text": "Tap this card to trigger a remote action.",
                                        "style": "bodySmall"
                                    }
                                ]
                            }
                        },
                        {
                            "type": "spacer",
                            "id": "bottom_spacer",
                            "size": 8
                        },
                        {
                            "type": "button",
                            "id": "btn_submit",
                            "title": "Submit Application",
                            "variant": "FILLED",
                            "is_full_width": true,
                            "actions": [
                                {
                                    "type": "submit_form",
                                    "endpoint": "/api/v1/onboarding"
                                }
                            ]
                        }
                    ]
                }
            }
            """.trimIndent()

            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            // 1. Data Layer DTO Parsing
            val dto = json.decodeFromString<HeimScreenResponseDto>(rawJson)
            // 2. Map to Pure Domain Layer Entity
            dto.toDomain()
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
                            val name = stateManager.getValue("user_name").ifBlank { "(unspecified name)" }
                            val email = stateManager.getValue("user_email").ifBlank { "(unspecified email)" }
                            scope.launch {
                                snackbarHostState.showSnackbar("Submitted to ${action.endpoint} - User: $name, Email: $email")
                            }
                        }
                        is NavigateAction -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("Navigating to: ${action.screenId}")
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
