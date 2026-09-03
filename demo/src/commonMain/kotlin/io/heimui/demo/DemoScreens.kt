package io.heimui.demo

/**
 * Catalog of rich Server-Driven UI screens for local testing, previewing, and demos.
 */
object DemoScreens {

    val screens = listOf(
        "showcase" to "Component Showcase",
        "ecommerce" to "E-Commerce Feed",
        "fintech" to "Fintech Form (KYC)",
        "profile" to "Profile & Settings"
    )

    fun getJson(screenId: String): String? {
        return when (screenId) {
            "showcase", "onboarding_showcase" -> SHOWCASE_JSON
            "ecommerce" -> ECOMMERCE_JSON
            "fintech" -> FINTECH_JSON
            "profile" -> PROFILE_JSON
            else -> null
        }
    }

    private val SHOWCASE_JSON = """
    {
        "id": "onboarding_showcase",
        "version": "1.0.0",
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
                    "text": "Server-Driven UI running natively on Kotlin Multiplatform & Compose with SWR Cache.",
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
                        { "type": "REQUIRED", "error_message": "Name is required" },
                        { "type": "MIN_LENGTH", "value": "3", "error_message": "Must be at least 3 characters" }
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
                        { "type": "REQUIRED", "error_message": "Email is required" },
                        { "type": "EMAIL", "error_message": "Invalid email format" }
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
                    "type": "text",
                    "id": "billing_title",
                    "text": "Billing Cycle",
                    "style": "titleMedium"
                },
                {
                    "type": "container",
                    "id": "billing_radio_row",
                    "direction": "HORIZONTAL",
                    "spacing": 16,
                    "children": [
                        {
                            "type": "radio",
                            "id": "radio_monthly",
                            "state_key": "billing_cycle",
                            "value": "monthly",
                            "label": "Monthly",
                            "initial_selected": true,
                            "accent_color": "primary"
                        },
                        {
                            "type": "radio",
                            "id": "radio_yearly",
                            "state_key": "billing_cycle",
                            "value": "yearly",
                            "label": "Yearly (-20%)",
                            "accent_color": "primary"
                        }
                    ]
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
                        { "type": "REQUIRED", "error_message": "Company name is required for enterprise accounts" }
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
                    "type": "image",
                    "id": "hero_image",
                    "url": "https://picsum.photos/600/300",
                    "blur_hash": "LEHV6nWB2yk8pyo0adR*.7kCMdnj",
                    "corner_radius": 12,
                    "height": 140,
                    "content_scale": "CROP"
                },
                {
                    "type": "container",
                    "id": "actions_row",
                    "direction": "HORIZONTAL",
                    "spacing": 8,
                    "children": [
                        {
                            "type": "button",
                            "id": "btn_sheet",
                            "title": "Open Sheet",
                            "variant": "OUTLINED",
                            "actions": [
                                {
                                    "type": "show_bottom_sheet",
                                    "title": "Quick Actions",
                                    "content": {
                                        "type": "container",
                                        "id": "sheet_container",
                                        "direction": "VERTICAL",
                                        "spacing": 12,
                                        "children": [
                                            {
                                                "type": "text",
                                                "id": "sheet_desc",
                                                "text": "This bottom sheet was rendered dynamically from Server-Driven UI!",
                                                "style": "bodyMedium"
                                            },
                                            {
                                                "type": "button",
                                                "id": "sheet_close_btn",
                                                "title": "Dismiss Sheet",
                                                "variant": "FILLED",
                                                "is_full_width": true,
                                                "actions": [
                                                    { "type": "dismiss_modal" }
                                                ]
                                            }
                                        ]
                                    }
                                }
                            ]
                        },
                        {
                            "type": "button",
                            "id": "btn_dialog",
                            "title": "Show Dialog",
                            "variant": "OUTLINED",
                            "actions": [
                                {
                                    "type": "show_dialog",
                                    "title": "Delete Confirmation",
                                    "message": "Are you sure you want to proceed with this operation?",
                                    "confirm_text": "Confirm",
                                    "confirm_actions": [
                                        { "type": "show_snackbar", "message": "Action confirmed!" },
                                        { "type": "dismiss_modal" }
                                    ],
                                    "dismiss_text": "Cancel"
                                }
                            ]
                        }
                    ]
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

    private val ECOMMERCE_JSON = """
    {
        "id": "ecommerce_feed",
        "version": "1.0.0",
        "title": "Store Discovery",
        "apply_safe_insets": true,
        "root": {
            "type": "container",
            "id": "ecom_root",
            "direction": "VERTICAL",
            "padding": 16,
            "spacing": 16,
            "children": [
                {
                    "type": "text",
                    "id": "ecom_header",
                    "text": "Featured Products",
                    "style": "headlineSmall",
                    "color": "primary"
                },
                {
                    "type": "card",
                    "id": "product_card_1",
                    "elevation": 3,
                    "corner_radius": 16,
                    "padding": 16,
                    "actions": [
                        { "type": "show_snackbar", "message": "Selected: Wireless Noise-Canceling Headphones" }
                    ],
                    "child": {
                        "type": "container",
                        "id": "product_1_content",
                        "direction": "VERTICAL",
                        "spacing": 8,
                        "children": [
                            {
                                "type": "image",
                                "id": "prod_img_1",
                                "url": "https://picsum.photos/500/250",
                                "blur_hash": "L6PZfSi_.AyE_3t7t7R**0o#DgR4",
                                "corner_radius": 8,
                                "height": 160
                            },
                            {
                                "type": "badge",
                                "id": "discount_badge",
                                "text": "25% OFF",
                                "background_color": "tertiaryContainer",
                                "text_color": "onTertiaryContainer"
                            },
                            {
                                "type": "text",
                                "id": "prod_title_1",
                                "text": "Wireless Noise-Canceling Headphones",
                                "style": "titleMedium"
                            },
                            {
                                "type": "text",
                                "id": "prod_price_1",
                                "text": "$249.99 USD",
                                "style": "titleLarge",
                                "color": "primary"
                            },
                            {
                                "type": "button",
                                "id": "btn_buy_1",
                                "title": "Add to Cart",
                                "variant": "FILLED",
                                "is_full_width": true,
                                "actions": [
                                    { "type": "show_snackbar", "message": "Added to cart successfully!" }
                                ]
                            }
                        ]
                    }
                }
            ]
        }
    }
    """.trimIndent()

    private val FINTECH_JSON = """
    {
        "id": "fintech_kyc",
        "version": "1.0.0",
        "title": "Account Opening (KYC)",
        "apply_safe_insets": true,
        "root": {
            "type": "container",
            "id": "kyc_root",
            "direction": "VERTICAL",
            "padding": 20,
            "spacing": 14,
            "children": [
                {
                    "type": "text",
                    "id": "kyc_title",
                    "text": "Identity & Compliance",
                    "style": "headlineSmall",
                    "color": "primary"
                },
                {
                    "type": "text",
                    "id": "kyc_subtitle",
                    "text": "Please provide your legal details for automated verification.",
                    "style": "bodyMedium",
                    "color": "onSurfaceVariant"
                },
                {
                    "type": "text_field",
                    "id": "tax_id_input",
                    "state_key": "tax_id",
                    "label": "Tax Identification Number (SSN/RFC/NIT)",
                    "placeholder": "123456789",
                    "input_type": "TEXT",
                    "validation_rules": [
                        { "type": "REQUIRED", "error_message": "Tax ID is required" },
                        { "type": "MIN_LENGTH", "value": "8", "error_message": "Minimum 8 characters" }
                    ]
                },
                {
                    "type": "text_field",
                    "id": "iban_input",
                    "state_key": "bank_account",
                    "label": "Bank Account / IBAN",
                    "placeholder": "ES9121000418450200051332",
                    "input_type": "TEXT",
                    "validation_rules": [
                        { "type": "REQUIRED", "error_message": "Bank account is required" },
                        { "type": "CUSTOM", "value": "IBAN", "error_message": "Invalid IBAN structure" }
                    ]
                },
                {
                    "type": "switch",
                    "id": "pep_switch",
                    "state_key": "is_pep",
                    "label": "Are you a Politically Exposed Person (PEP)?",
                    "initial_checked": false
                },
                {
                    "type": "button",
                    "id": "kyc_submit_btn",
                    "title": "Verify & Create Account",
                    "variant": "FILLED",
                    "is_full_width": true,
                    "actions": [
                        { "type": "submit_form", "endpoint": "/api/v1/compliance/verify" }
                    ]
                }
            ]
        }
    }
    """.trimIndent()

    private val PROFILE_JSON = """
    {
        "id": "profile_settings",
        "version": "1.0.0",
        "title": "Account Settings",
        "apply_safe_insets": true,
        "root": {
            "type": "container",
            "id": "profile_root",
            "direction": "VERTICAL",
            "padding": 20,
            "spacing": 16,
            "children": [
                {
                    "type": "container",
                    "id": "profile_header",
                    "direction": "HORIZONTAL",
                    "alignment": "CENTER",
                    "spacing": 16,
                    "children": [
                        {
                            "type": "icon",
                            "id": "user_avatar",
                            "name": "person",
                            "tint": "primary",
                            "size": 48
                        },
                        {
                            "type": "container",
                            "id": "user_info",
                            "direction": "VERTICAL",
                            "spacing": 2,
                            "children": [
                                { "type": "text", "id": "user_name_text", "text": "Julian Velandia", "style": "titleMedium" },
                                { "type": "text", "id": "user_email_text", "text": "julian@heimui.io", "style": "bodySmall", "color": "onSurfaceVariant" }
                            ]
                        }
                    ]
                },
                {
                    "type": "divider",
                    "id": "prof_divider",
                    "thickness": 1
                },
                {
                    "type": "switch",
                    "id": "push_notifications",
                    "state_key": "push_enabled",
                    "label": "Push Notifications",
                    "initial_checked": true
                },
                {
                    "type": "switch",
                    "id": "biometric_auth",
                    "state_key": "biometrics_enabled",
                    "label": "Biometric Authentication",
                    "initial_checked": true
                },
                {
                    "type": "button",
                    "id": "logout_btn",
                    "title": "Log Out",
                    "variant": "OUTLINED",
                    "is_full_width": true,
                    "actions": [
                        { "type": "show_snackbar", "message": "User logged out." }
                    ]
                }
            ]
        }
    }
    """.trimIndent()
}
