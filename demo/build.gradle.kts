import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Demo/showcase module. Deliberately NOT published: it exists so the sample apps have something
 * to render, and shipping it inside `:shared` meant 600+ lines of demo JSON and a demo Compose
 * app were downloaded by every SDK consumer -- and exposed in the iOS ObjC header.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "HeimUIDemo"
            isStatic = true
            // The demo framework must re-export the SDK so Swift can see HeimUI's symbols.
            export(project(":shared"))
        }
    }

    android {
        namespace = "io.heimui.demo"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}
