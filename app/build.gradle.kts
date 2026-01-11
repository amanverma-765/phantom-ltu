import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.agp.app)
    alias(phantom.plugins.kotlin.android)
    alias(phantom.plugins.kotlin.compose)
    alias(phantom.plugins.serialization)
    alias(phantom.plugins.ksp)
    alias(phantom.plugins.room)
}

android {
    namespace = "com.navi.phantom"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.navi.phantom"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Test
    testImplementation(phantom.junit)
    androidTestImplementation(phantom.androidx.junit)
    androidTestImplementation(phantom.androidx.espresso.core)
    androidTestImplementation(platform(phantom.androidx.compose.bom))
    androidTestImplementation(phantom.androidx.compose.ui.test.junit4)
    debugImplementation(phantom.androidx.compose.ui.tooling)
    debugImplementation(phantom.androidx.compose.ui.test.manifest)
    // Core
    implementation(phantom.androidx.core.ktx)
    implementation(phantom.androidx.lifecycle.runtime.ktx)
    implementation(phantom.androidx.activity.compose)
    // Compose
    implementation(platform(phantom.androidx.compose.bom))
    implementation(phantom.androidx.compose.ui)
    implementation(phantom.androidx.compose.ui.graphics)
    implementation(phantom.androidx.compose.ui.tooling.preview)
    implementation(phantom.androidx.compose.material3)
    implementation(phantom.androidx.compose.material.icons.extended)
    implementation(phantom.androidx.compose.material3.adaptive.navigation.suite)
    // Navigation
    implementation(phantom.androidx.navigation3.runtime)
    implementation(phantom.androidx.navigation3.ui)
    implementation(phantom.navigation3.viewmodel)
    // DI
    implementation(phantom.koin.core)
    implementation(phantom.koin.android)
    implementation(phantom.koin.compose)
    implementation(phantom.koin.compose.viewmodel)
    implementation(phantom.koin.navigation3)
    // Credential Manager
    implementation(phantom.androidx.credentials)
    implementation(phantom.androidx.credentials.play.services.auth)
    // Google ID helper library
    implementation(phantom.googleid)
    // Ktor
    implementation(phantom.ktor.client.core)
    implementation(phantom.ktor.client.okhttp)
    implementation(phantom.ktor.client.content.negotiation)
    implementation(phantom.ktor.serialization.json)
    implementation(phantom.ktor.client.logging)
    implementation(phantom.ktor.client.auth)
    // DataStore
    implementation(phantom.datastore.preferences)
    // SplashScreen
    implementation(phantom.androidx.splashscreen)
    // Coil
    implementation(phantom.coil.compose)
    implementation(phantom.coil.network)
    // Other
    implementation(phantom.kotlinx.serialization.json)
    // Room Database
    implementation(phantom.androidx.room.runtime)
    implementation(phantom.androidx.room.ktx)
    ksp(phantom.androidx.room.compiler)
    // Logging
    implementation(phantom.kermit)
    // Shared modules
}