import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
}

android {
    namespace = "com.riva.mods"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "org.lsposed.lspatch"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField(
            type = "String",
            name = "GOOGLE_WEB_CLIENT_ID",
            value = properties.getProperty("GOOGLE_WEB_CLIENT_ID")
        )
        buildConfigField(
            type = "String",
            name = "BASE_URL",
            value = properties.getProperty("BASE_URL")
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.navigation3.viewmodel)
    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.navigation3)
    // Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    // Google ID helper library
    implementation(libs.googleid)
    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.auth)
    // DataStore
    implementation(libs.datastore.preferences)
    // SplashScreen
    implementation(libs.androidx.splashscreen)
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    // Other
    implementation(libs.kotlinx.serialization.json)
    // Shared modules
    implementation(project(":shared"))
    implementation(project(":xposed-api"))
}

// Validate LSPatch assets before build
val requiredAssets = listOf(
    "lspatch/loader.dex",
    "lspatch/so/arm64-v8a/liblspatch.so",
    "lspatch/so/armeabi-v7a/liblspatch.so",
    "lspatch/so/x86/liblspatch.so",
    "lspatch/so/x86_64/liblspatch.so"
)

tasks.register("validateLspatchAssets") {
    doLast {
        val assetsDir = file("src/main/assets")
        val missing = requiredAssets.filter { !File(assetsDir, it).exists() }

        if (missing.isNotEmpty()) {
            throw GradleException("Missing LSPatch assets: ${missing.joinToString()}")
        }
        println("✓ LSPatch assets validated")
    }
}

tasks.named("preBuild") { dependsOn("validateLspatchAssets") }