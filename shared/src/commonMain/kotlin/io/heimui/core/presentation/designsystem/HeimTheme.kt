package io.heimui.core.presentation.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.presentation.action.HeimActionDispatcher
import io.heimui.core.presentation.action.LocalHeimActionDispatcher
import io.heimui.core.presentation.imageloader.CoilHeimImageLoader
import io.heimui.core.presentation.imageloader.HeimImageLoader
import io.heimui.core.presentation.imageloader.LocalHeimImageLoader
import io.heimui.core.presentation.launcher.ComposeUriUrlLauncher
import io.heimui.core.presentation.launcher.HeimUrlLauncher
import io.heimui.core.presentation.launcher.HeimUrlPolicy
import io.heimui.core.presentation.launcher.LocalHeimUrlLauncher
import io.heimui.core.presentation.modal.DefaultHeimModalPresenter
import io.heimui.core.presentation.modal.HeimModalPresenter
import io.heimui.core.presentation.modal.LocalHeimModalPresenter
import io.heimui.core.presentation.registry.HeimCustomComponentRegistry
import io.heimui.core.presentation.registry.LocalHeimCustomComponentRegistry
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.HeimTelemetryObserver
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import io.heimui.core.presentation.telemetry.NoOpHeimTelemetryObserver
import io.heimui.core.presentation.tracking.HeimTrackingDispatcher
import io.heimui.core.presentation.tracking.LocalHeimTrackingDispatcher
import io.heimui.core.presentation.tracking.NoOpHeimTrackingDispatcher
import io.heimui.core.presentation.state.HeimFormDraftStorage
import io.heimui.core.presentation.state.LocalHeimFormDraftStorage
import io.heimui.core.presentation.validation.LocalHeimValidatorRegistry

/**
 * Installs the theme and every pluggable contract HeimUI renderers read from the composition.
 *
 * Wrap your HeimUI content in this. Without it the renderers fall back to inert defaults: URLs
 * do not open, brand tokens are ignored, and no telemetry is emitted.
 *
 * ```kotlin
 * HeimTheme(
 *     colorScheme = MyAppTheme.colors,
 *     brandTokens = HeimBrandTokens.build {
 *         color("brand_primary", Color(0xFF6750A4))
 *         textStyle("price", MaterialTheme.typography.headlineSmall)
 *     },
 *     telemetryObserver = { event -> analytics.log(event) },
 * ) {
 *     HeimScreen(screenId = "home", onAction = ::handleAction)
 * }
 * ```
 *
 * @param darkTheme whether to use the dark palette. Defaults to the system setting.
 * @param colorScheme overrides the Material 3 palette. `null` uses the platform default for
 *   [darkTheme].
 * @param typography overrides the Material 3 type scale.
 * @param shapes overrides the Material 3 shape scale. SDK surfaces such as the error card and
 *   dialogs read it, so supplying it keeps them consistent with the host app.
 * @param iconProvider draws icons requested by name in the payload. Replace it to use your own
 *   icon set instead of the built-in vector suite.
 * @param brandTokens custom semantic color and text-style names a payload may reference, resolved
 *   ahead of the Material tokens.
 * @param imageLoader engine used for remote images. Replace to use Glide, Kamel, SDWebImage or a
 *   shared cache.
 * @param modalPresenter renders bottom sheets and dialogs. Replace to match your own modal style.
 * @param urlLauncher opens URLs from `OpenUrlAction`. Defaults to the platform handler, filtered
 *   by [urlPolicy].
 * @param urlPolicy schemes a payload is allowed to open. Defaults to https only; add your own
 *   deep-link scheme to enable in-app navigation links.
 * @param formDraftStorage persists in-progress form input so it survives process death. `null`
 *   disables persistence. Supply an encrypted driver for PII or financial forms.
 * @param actionDispatcher interceptor chain run before each action, for auth checks, logging or
 *   action rewriting.
 * @param validatorRegistry custom validators referenced by `CUSTOM` validation rules. A rule
 *   naming a validator that is not registered **fails closed**.
 * @param telemetryObserver receives render, action, error and payload-violation events.
 * @param customComponentRegistry native composables for `CustomComponent`s, keyed by name.
 *
 * @see HeimScreen
 * @see HeimBrandTokens
 */
@Composable
public fun HeimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme? = null,
    typography: Typography? = null,
    shapes: Shapes? = null,
    iconProvider: HeimIconProvider = DefaultHeimIconProvider,
    brandTokens: HeimBrandTokens = HeimBrandTokens.default,
    imageLoader: HeimImageLoader = remember { CoilHeimImageLoader() },
    modalPresenter: HeimModalPresenter = remember { DefaultHeimModalPresenter() },
    urlLauncher: HeimUrlLauncher? = null,
    /** Scheme allow-list for payload-supplied URLs. Defaults to https only. */
    urlPolicy: HeimUrlPolicy = HeimUrlPolicy.default,
    /** Opt-in form-draft persistence. Supply an encrypted driver for PII or financial forms. */
    formDraftStorage: HeimFormDraftStorage? = null,
    actionDispatcher: HeimActionDispatcher = remember { HeimActionDispatcher() },
    validatorRegistry: HeimValidatorRegistry = remember { HeimValidatorRegistry() },
    telemetryObserver: HeimTelemetryObserver = NoOpHeimTelemetryObserver,
    /**
     * Where a payload's `tracking` goes. Distinct from [telemetryObserver], which is the SDK
     * reporting on itself — this is the product reporting on the user, with names the payload owns.
     */
    trackingDispatcher: HeimTrackingDispatcher = NoOpHeimTrackingDispatcher,
    customComponentRegistry: HeimCustomComponentRegistry = remember { HeimCustomComponentRegistry() },
    content: @Composable () -> Unit
) {
    val defaultColorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    val finalColorScheme = colorScheme ?: defaultColorScheme
    val finalTypography = typography ?: Typography()
    val finalShapes = shapes ?: Shapes()
    val uriHandler = LocalUriHandler.current
    val effectiveUrlLauncher = urlLauncher ?: remember(uriHandler, urlPolicy, telemetryObserver) {
        ComposeUriUrlLauncher(
            uriHandler = uriHandler,
            policy = urlPolicy,
            onRefused = { url, reason ->
                telemetryObserver.onEvent(HeimTelemetryEvent.UrlBlocked(url, reason))
            }
        )
    }

    CompositionLocalProvider(
        LocalHeimIconProvider provides iconProvider,
        LocalHeimBrandTokens provides brandTokens,
        LocalHeimImageLoader provides imageLoader,
        LocalHeimModalPresenter provides modalPresenter,
        LocalHeimUrlLauncher provides effectiveUrlLauncher,
        LocalHeimActionDispatcher provides actionDispatcher,
        LocalHeimTelemetryObserver provides telemetryObserver,
        LocalHeimTrackingDispatcher provides trackingDispatcher,
        LocalHeimCustomComponentRegistry provides customComponentRegistry,
        LocalHeimValidatorRegistry provides validatorRegistry,
        LocalHeimFormDraftStorage provides formDraftStorage
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = finalTypography,
            shapes = finalShapes,
            content = content
        )
    }
}
