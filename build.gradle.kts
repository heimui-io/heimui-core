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
 *   ./gradlew publishLocal                              → <version>-LOCAL
 *   ./gradlew publishToMavenLocal                       → <version>  (overwrites the release)
 *   ./gradlew publishToMavenLocal -Pheimui.version=0.0.2-alpha
 *
 * The version is whatever `:shared` declares, so these are written as `<version>` rather than
 * spelled out -- a worked example here goes stale on the next release and hands whoever copies it
 * a coordinate that does not resolve.
 *
 * Asking for both in one invocation fails on purpose: a build has a single version, so
 * `publishToMavenLocal publishLocal` would publish each of them as -LOCAL and leave the release
 * coordinate silently stale.
 *
 * Then in the consuming project, with `mavenLocal()` among its repositories:
 *
 *   implementation("io.heimui:heimui-core:<version>-LOCAL")
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

// Declared as `Exec` tasks rather than run from inside an action: `project.exec` at execution time
// is incompatible with the configuration cache, which this build has switched on.
val checkConsumerRules by tasks.registering(Exec::class) {
    group = "verification"
    description = "Open the built AAR and confirm it ships the R8 keep rules."
    commandLine("./scripts/check-consumer-rules.sh")
}

val checkNotice by tasks.registering(Exec::class) {
    group = "verification"
    description = "Confirm every shipped dependency appears in NOTICE."
    commandLine("./scripts/check-notice.sh")
}

val checkSchema by tasks.registering(Exec::class) {
    group = "verification"
    description = "Confirm the screen schema is valid JSON."
    commandLine("python3", "-c", "import json; json.load(open('schema/heimui-screen.schema.json'))")
}

/**
 * Everything CI checks, in one task, so it can be run before pushing rather than after.
 *
 * The two script guards are part of that promise: they check the built artifact and the derived
 * attribution list, neither of which any Gradle task verifies. Leaving them out would make this
 * task green while CI is red, which is worse than not having it.
 */
tasks.register("verify") {
    group = "verification"
    description = "Run everything CI runs: tests, ABI, keep rules, attributions and the schema."
    dependsOn(":shared:allTests", ":shared:apiCheck", checkConsumerRules, checkNotice, checkSchema)
}
