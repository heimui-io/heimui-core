package io.heimui.core

import io.heimui.core.di.HeimConfig
import io.heimui.core.di.createHeimCoreModule
import io.heimui.core.domain.repository.HeimScreenRepository
import io.ktor.client.HttpClient
import kotlin.concurrent.Volatile
import org.koin.core.Koin
import org.koin.dsl.koinApplication

/**
 * Entry point of the HeimUI SDK.
 *
 * Initialize once during app startup, then render server-driven screens with
 * [io.heimui.core.presentation.HeimScreen].
 *
 * **Android** — in `Application.onCreate`:
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         HeimUI.initialize(
 *             HeimConfig(
 *                 baseUrl = "https://api.example.com/sdui",
 *                 authTokenProvider = { session.bearerToken },
 *             )
 *         )
 *     }
 * }
 * ```
 *
 * **iOS** — in your `AppDelegate` or `App` initializer:
 * ```swift
 * HeimUI.shared.initialize(config: HeimConfig(baseUrl: "https://api.example.com/sdui", ...))
 * ```
 *
 * Screens can also be rendered without initializing at all, by passing a repository directly to
 * `HeimScreen(repository = ...)`. That is the recommended path for previews and tests.
 *
 * ### Threading
 * State is `@Volatile`, so a write from the initializing thread is visible to every reader.
 * That is the guarantee this object needs: [initialize] is called once at startup and everything
 * afterwards reads. Calling [initialize] concurrently from two threads is a host bug and is not
 * defended against.
 *
 * @see HeimConfig
 * @see io.heimui.core.presentation.HeimScreen
 */
public object HeimUI {
    @Volatile
    private var koinInstance: Koin? = null

    @Volatile
    private var activeConfig: HeimConfig? = null

    @Volatile
    private var ownedHttpClient: HttpClient? = null

    /** Whether [initialize] has been called and not since [reset]. Never throws. */
    public val isInitialized: Boolean
        get() = koinInstance != null

    /**
     * The active configuration.
     *
     * @throws IllegalStateException if the SDK has not been initialized. Guard with
     *   [isInitialized] when calling from code that may run before app startup completes.
     */
    public val config: HeimConfig
        get() = activeConfig ?: notInitialized()

    /**
     * The shared repository backing every [io.heimui.core.presentation.HeimScreen] that does not
     * receive one explicitly.
     *
     * @throws IllegalStateException if the SDK has not been initialized.
     */
    public val repository: HeimScreenRepository
        get() = koinInstance?.get<HeimScreenRepository>() ?: notInitialized()

    /**
     * Initializes the SDK with [config].
     *
     * Safe to call more than once: the previous instance is replaced and the resources it owned
     * (its HTTP client and connection pool) are released first. A client supplied through
     * [HeimConfig.customHttpClient] belongs to the host and is never closed by the SDK.
     *
     * @param config connection, security and storage settings. See [HeimConfig].
     */
    public fun initialize(config: HeimConfig) {
        closeOwnedResources()
        val app = koinApplication { modules(createHeimCoreModule(config)) }
        // Only a client the SDK created is ours to close; a host-supplied one is not.
        ownedHttpClient = if (config.customHttpClient == null) app.koin.get<HttpClient>() else null
        activeConfig = config
        koinInstance = app.koin
    }

    /**
     * Tears the SDK down and closes the resources it owns.
     *
     * Intended for tests and for hosts that reconfigure at runtime (for example on sign-out,
     * to drop a stale auth token and its cached screens). After this, [isInitialized] is `false`
     * and [repository] throws until [initialize] is called again.
     */
    public fun reset() {
        closeOwnedResources()
        koinInstance?.close()
        koinInstance = null
        activeConfig = null
    }

    private fun closeOwnedResources() {
        ownedHttpClient?.close()
        ownedHttpClient = null
    }

    private fun notInitialized(): Nothing =
        error("HeimUI is not initialized. Call HeimUI.initialize(config) in your Application or AppDelegate.")
}
