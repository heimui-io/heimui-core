import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("maven-publish")
}

group = "io.heimui"
version = "0.0.1-alpha"

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
            // Not "Shared": that is the KMP wizard default, so it is the name most likely to
            // collide with a framework the host app already links.
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
            implementation(libs.compose.uiToolingPreview)
            implementation(compose.uiTooling)
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

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("HeimUI Core")
            description.set("Engine for Server-Driven UI in Kotlin Multiplatform & Compose")
            url.set("https://github.com/julianvelandia23/heimui-core")
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
                connection.set("scm:git:git://github.com/julianvelandia23/heimui-core.git")
                developerConnection.set("scm:git:ssh://github.com:julianvelandia23/heimui-core.git")
                url.set("https://github.com/julianvelandia23/heimui-core")
            }
        }
    }
}
