plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.binaryCompatibility)
}

apiValidation {
    // Only :shared is published, so only its ABI is a contract.
    ignoredProjects.addAll(listOf("androidApp", "demo"))

    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        // The published targets are Android and iOS; without this only JVM ABI would be tracked,
        // which for this module is nothing.
        enabled = true
    }
}