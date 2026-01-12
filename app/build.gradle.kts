val defaultManagerPackageName: String by rootProject.extra
val verCode: Int by rootProject.extra
val verName: String by rootProject.extra

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

    defaultConfig {
        applicationId = defaultManagerPackageName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        noCompress.add(".so")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        all {
            sourceSets[name].assets.srcDirs(rootProject.projectDir.resolve("out/assets/$name"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val variantLowered = variant.name.lowercase()
        val variantCapped = variant.name.replaceFirstChar { it.uppercase() }

        val copyAssetsTask = tasks.register<Copy>("copy${variantCapped}Assets") {
            dependsOn(":meta-loader:copy$variantCapped")
            dependsOn(":patch-loader:copy$variantCapped")

            into(layout.buildDirectory.dir("intermediates/assets/$variantLowered/merge${variantCapped}Assets"))
            from(rootProject.projectDir.resolve("out/assets/${variant.name}"))
        }

        tasks.named("merge${variantCapped}Assets").configure {
            dependsOn(copyAssetsTask)
        }

        tasks.register<Copy>("build$variantCapped") {
            dependsOn(tasks["assemble$variantCapped"])
            from(variant.outputs.map { it.outputFile })
            into(rootProject.projectDir.resolve("out/$variantLowered"))
            rename(".*.apk", "phantom-ltu-v$verName-$verCode-$variantLowered.apk")
        }
    }
}

dependencies {
    // Shared modules
    implementation(projects.shared.android)
    implementation(projects.shared.java)

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

    // Serialization
    implementation(phantom.kotlinx.serialization.json)

    // Room Database
    implementation(phantom.androidx.room.runtime)
    implementation(phantom.androidx.room.ktx)
    ksp(phantom.androidx.room.compiler)

    // Logging
    implementation(phantom.kermit)
}
