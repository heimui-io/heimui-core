package io.heimui.core.presentation.validation

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import io.heimui.core.domain.evaluator.HeimValidatorRegistry

/**
 * Scoped validator registry for the composition.
 *
 * Previously the only way to register a CUSTOM validator was to mutate the global
 * `HeimValidatorRegistry.default` singleton, which leaked between SDK instances and made test
 * isolation impossible. Now [io.heimui.core.presentation.designsystem.HeimTheme] provides a
 * per-host instance and the form renderers read it from here.
 */
public val LocalHeimValidatorRegistry: ProvidableCompositionLocal<HeimValidatorRegistry> =
    staticCompositionLocalOf {
    HeimValidatorRegistry.default
}
