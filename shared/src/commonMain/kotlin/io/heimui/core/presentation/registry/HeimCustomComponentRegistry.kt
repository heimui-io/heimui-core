package io.heimui.core.presentation.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.CustomComponent
import io.heimui.core.presentation.state.HeimStateManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.model.toJsonElement
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.LocalHeimTelemetryObserver
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

public typealias CustomComponentRenderer = @Composable (
    component: CustomComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier
) -> Unit

/**
 * Registry allowing host applications to register native Composables for custom SDUI components.
 */
public class HeimCustomComponentRegistry {
    private val renderers = mutableMapOf<String, CustomComponentRenderer>()

    public fun register(name: String, renderer: CustomComponentRenderer): HeimCustomComponentRegistry {
        renderers[name] = renderer
        return this
    }

    public fun getRenderer(name: String): CustomComponentRenderer? {
        return renderers[name]
    }

    public companion object {
        public fun build(builder: HeimCustomComponentRegistry.() -> Unit): HeimCustomComponentRegistry {
            return HeimCustomComponentRegistry().apply(builder)
        }
    }
}

public val LocalHeimCustomComponentRegistry: ProvidableCompositionLocal<HeimCustomComponentRegistry> =
    staticCompositionLocalOf {
    HeimCustomComponentRegistry()
}

/**
 * Registers a custom component whose `data` is decoded into a type you declare.
 *
 * The untyped [HeimCustomComponentRegistry.register] hands you a `Map<String, HeimValue>` and
 * leaves the unpacking to you, which reads like this at every call site:
 *
 * ```kotlin
 * title = d["title"]?.asString.orEmpty()
 * price = d["price"]?.asDouble ?: 0.0
 * ```
 *
 * That is a default per field, repeated per component, with the payload's shape spelled out in
 * string literals the compiler cannot check. A typo in `"image_url"` is a silently empty image.
 *
 * Declaring the shape once as a `@Serializable` class moves all of it to the type system: defaults
 * live in the constructor, names are checked by the serializer, and the class doubles as the
 * contract you hand to whoever writes the payload.
 *
 * ```kotlin
 * @Serializable
 * data class ProductCard(
 *     val sku: String,
 *     val title: String,
 *     val price: Double,
 *     val currency: String = "USD",
 *     @SerialName("image_url") val imageUrl: String? = null,
 *     @SerialName("in_stock") val inStock: Boolean = true,
 * )
 *
 * registry.register<ProductCard>("HORIZONTAL_CARD_PRODUCT") { product, onAction, modifier ->
 *     HorizontalProductCard(
 *         product = product,
 *         onClick = { onAction(NavigateAction("product_detail", mapOf("sku" to product.sku))) },
 *         modifier = modifier,
 *     )
 * }
 * ```
 *
 * A payload that does not fit the type renders **nothing** and reports a `PayloadViolation`,
 * rather than throwing. The same rule the rest of the SDK follows: a malformed component costs its
 * own space on screen, never the screen around it.
 */
public inline fun <reified T> HeimCustomComponentRegistry.register(
    name: String,
    noinline renderer: @Composable (data: T, onAction: (HeimAction) -> Unit, modifier: Modifier) -> Unit
): HeimCustomComponentRegistry = registerTyped(name, serializer(), renderer)

/**
 * The non-inline half of [register], so the reified overload stays public without exposing the
 * SDK's Json instance.
 */
@PublishedApi
internal fun <T> HeimCustomComponentRegistry.registerTyped(
    name: String,
    deserializer: DeserializationStrategy<T>,
    renderer: @Composable (data: T, onAction: (HeimAction) -> Unit, modifier: Modifier) -> Unit
): HeimCustomComponentRegistry = register(name) { component, _, onAction, modifier ->
    val telemetry = LocalHeimTelemetryObserver.current
    val decoded = remember(component) {
        runCatching {
            HeimJson.instance.decodeFromJsonElement(
                deserializer,
                JsonObject(component.data.mapValues { it.value.toJsonElement() })
            )
        }.getOrNull()
    }

    if (decoded == null) {
        // Reported rather than thrown, and reported once: the backend team finds out its payload
        // does not fit the type the app declared, and the user sees the rest of the screen.
        LaunchedEffect(component.id) {
            telemetry.onEvent(
                HeimTelemetryEvent.PayloadViolation(
                    screenId = component.id,
                    violations = listOf("custom '$name' does not match the type the app registered")
                )
            )
        }
        return@register
    }

    renderer(decoded, onAction, modifier)
}
