package io.heimui.core

import io.heimui.core.di.HeimConfig
import io.heimui.core.di.createHeimCoreModule
import io.heimui.core.domain.repository.HeimScreenRepository
import org.koin.core.Koin
import org.koin.dsl.koinApplication

/**
 * Public facade and entrypoint for initializing and accessing HeimUI across platforms.
 */
object HeimUI {
    private var koinInstance: Koin? = null
    private var activeConfig: HeimConfig? = null

    /**
     * Checks if HeimUI has been initialized.
     */
    val isInitialized: Boolean
        get() = koinInstance != null

    /**
     * Active configuration.
     */
    val config: HeimConfig
        get() = activeConfig ?: error("HeimUI is not initialized. Call HeimUI.initialize(config) in your Application or AppDelegate.")

    /**
     * Shared repository instance managed by HeimUI.
     */
    val repository: HeimScreenRepository
        get() = koinInstance?.get<HeimScreenRepository>()
            ?: error("HeimUI is not initialized. Call HeimUI.initialize(config) in your Application or AppDelegate.")

    /**
     * Initializes HeimUI with the provided configuration.
     */
    fun initialize(config: HeimConfig) {
        activeConfig = config
        val app = koinApplication {
            modules(createHeimCoreModule(config))
        }
        koinInstance = app.koin
    }

    /**
     * Resets the HeimUI instance (useful for testing).
     */
    fun reset() {
        koinInstance = null
        activeConfig = null
    }
}
