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
/**
 * Publishes the SDK to the local Maven repository so other projects on this machine can consume
 * an unreleased build.
 *
 * Registered at the root, not on `:shared`, so it shows up in the IDE's Gradle panel under
 * heimui-core → Tasks → publishing rather than buried one level down.
 *
 *   ./gradlew publishLocal                              → 0.0.1-alpha-LOCAL
 *   ./gradlew publishToMavenLocal                       → 0.0.1-alpha  (overwrites the release)
 *   ./gradlew publishToMavenLocal -Pheimui.version=0.0.2-alpha
 *
 * Then in the consuming project:
 *
 *   implementation("io.heimui:heimui-core:0.0.1-alpha-LOCAL")
 *
 * The `-LOCAL` suffix is the point. Publishing over the released coordinate leaves no way to tell
 * which build a failure came from, and every project on the machine silently picks up whatever
 * was published last.
 */
tasks.register("publishLocal") {
    group = "publishing"
    description = "Publish the SDK to mavenLocal as <version>-LOCAL, alongside any released build."
    dependsOn(":shared:publishLocal")
}

/** Everything CI checks, in one task, so it can be run before pushing rather than after. */
tasks.register("verify") {
    group = "verification"
    description = "Run the full test suite and the binary-compatibility check."
    dependsOn(":shared:allTests", "apiCheck")
}
