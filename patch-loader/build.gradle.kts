plugins {
    alias(libs.plugins.agp.app)
    alias(phantom.plugins.kotlin.android)
}

android {
    namespace = "com.navi.phantom.loader"

    defaultConfig {
        multiDexEnabled = false
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.uppercase() }
    val variantLowered = variant.name.lowercase()

    val copyDexTask = tasks.register<Copy>("copyDex$variantCapped") {
        dependsOn("assemble$variantCapped")
        from(layout.buildDirectory.dir("intermediates/dex/$variantLowered/mergeDex$variantCapped"))
        include("classes.dex")
        rename("classes.dex", "loader.dex")
        into(rootProject.layout.projectDirectory.dir("out/assets/${variant.name}/phantom"))
    }

    tasks.register("copy$variantCapped") {
        dependsOn(copyDexTask)

        doLast {
            println("Dex file has been copied to ${rootProject.layout.projectDirectory.dir("out")}")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.core)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.shared.java)
    implementation(phantom.kermit)
}