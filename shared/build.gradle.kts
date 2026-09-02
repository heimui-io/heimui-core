import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    id("signing")
}

group = "io.heimui"

/**
 * The published version.
 *
 * Overridable so a local build can be consumed alongside a released one. Without this, testing an
 * unreleased change means overwriting `0.0.1-alpha` in the local Maven cache with something that
 * is not what the coordinate says — and every project on the machine silently picks it up.
 *
 *   ./gradlew publishToMavenLocal                                → 0.0.1-alpha-1
 *   ./gradlew publishToMavenLocal -Pheimui.version=0.0.1-alpha-2 → 0.0.1-alpha-2
 *   ./gradlew publishLocal                                       → 0.0.1-alpha-1-LOCAL
 *
 * The `-alpha-N` suffix is ordered by both Maven and Gradle: alpha-1 sorts before alpha-2, and
 * every alpha sorts before the eventual 0.0.1 release. A bare `-alpha` would leave nowhere to go
 * for the second one.
 */
version = (findProperty("heimui.version") as String?) ?: "0.0.1-alpha-1"

kotlin {
    // Forces an explicit visibility modifier and return type on every public declaration.
    // Without it every internal helper is part of the published contract, and any refactor is a
    // potential breaking change for consumers that nobody notices.
    explicitApi()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            // The framework name is global to the host app's link step, so a generic one risks
            // colliding with something else it already links.
            baseName = "HeimUI"
            isStatic = true
        }
    }
    
    android {
       namespace = "io.heimui.core.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       optimization {
           consumerKeepRules.files.add(file("consumer-rules.pro"))
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            // Deliberately absent: `ui-tooling` and `ui-tooling-preview`. The SDK declares no
            // @Preview of its own, and an `implementation` dependency in a KMP library publishes
            // as `runtime` scope — so every consumer's *release* APK was carrying the Compose
            // inspector. Tooling belongs in the app's `debugImplementation`, not in a library.
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
            api(libs.kotlinx.coroutines.core)
            api(libs.ktor.client.core)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            // Civil-calendar maths for `date_picker`. Hand-rolling epoch-to-date is how an SDK
            // ends up with a bug that only appears on 29 February.
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        iosTest.dependencies {
            // Compose UI tests run on the iOS simulator; the Android host target is a bare JVM
            // and would need Robolectric to host a composition.
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

/**
 * Publishes under a `-LOCAL` suffix so a consumer can point at an unreleased build explicitly.
 *
 * The suffix is the point. Overwriting the released coordinate leaves no way to tell which build
 * a failure came from, and no way back except a clean of `~/.m2` — a suffix makes the choice
 * visible in the consumer's `build.gradle.kts` instead of hidden in a cache.
 */
val publishLocal by tasks.registering {
    group = "publishing"
    description = "Publishes to mavenLocal as <version>-LOCAL, alongside any released build."
    dependsOn(tasks.named("publishToMavenLocal"))

    // The coordinate is captured now, not read inside the action. The configuration cache cannot
    // serialise a reference back into the build script, and reading `version` at execution time
    // is exactly that.
    val coordinate = "io.heimui:heimui-core:$version"
    doLast { println("Published $coordinate to mavenLocal") }
}

// The suffix has to be applied before the publication is configured, so it is decided here rather
// than inside the task's action -- a task cannot change the version it is publishing.
run {
    val requested = gradle.startParameter.taskNames
    val wantsLocal = requested.any { it.substringAfterLast(':') == "publishLocal" }
    val wantsRelease = requested.any { it.substringAfterLast(':') == "publishToMavenLocal" }

    // Both in one invocation is a trap, and it bit once already: the version is a single value
    // for the whole build, so `publishToMavenLocal publishLocal` published *both* under -LOCAL
    // and left the release coordinate silently stale. Failing beats publishing the wrong thing
    // under the right name.
    require(!(wantsLocal && wantsRelease)) {
        "Run `publishLocal` and `publishToMavenLocal` separately. A build has one version, so " +
            "asking for both publishes each of them as -LOCAL and leaves $version stale."
    }

    if (wantsLocal && findProperty("heimui.version") == null) {
        version = "$version-LOCAL"
    }
}

mavenPublishing {
    // Central Portal, not the retired OSSRH endpoint.
    publishToMavenCentral()

    // Central rejects unsigned artifacts.
    signAllPublications()

    // Without this, artifacts would be named after the Gradle module, which is `shared`.
    coordinates("io.heimui", "heimui-core", version.toString())

    pom {
        name.set("HeimUI Core")
        description.set("Engine for Server-Driven UI in Kotlin Multiplatform & Compose")
        url.set("https://github.com/heimui-io/heimui-core")
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("julianvelandia")
                name.set("Julian Velandia")
                organization.set("HeimUI")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/heimui-io/heimui-core.git")
            developerConnection.set("scm:git:ssh://github.com:heimui-io/heimui-core.git")
            url.set("https://github.com/heimui-io/heimui-core")
        }
    }
}

// Sign by shelling out to `gpg` rather than handing Gradle the key material: the private key stays
// in the local keyring, and only its id and passphrase live in ~/.gradle/gradle.properties, which
// is outside the repo. Without this line the signing plugin finds no signatory and every
// `sign*Publication` task fails, because `signing.gnupg.*` is only read in gpg-command mode.
signing {
    useGpgCmd()
}
