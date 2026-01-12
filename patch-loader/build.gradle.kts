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

    val copySoTask = tasks.register<Copy>("copySo$variantCapped") {
        dependsOn("assemble$variantCapped")
        dependsOn("strip${variantCapped}DebugSymbols")
        val libDir = variantLowered + "/strip${variantCapped}DebugSymbols"
        from(
            fileTree(
                layout.buildDirectory.dir("intermediates/stripped_native_libs/$libDir/out/lib")
            ) {
                include("**/libphantom.so")
            }
        )
        into(rootProject.layout.projectDirectory.dir("out/assets/${variant.name}/phantom/so"))
    }

    tasks.register("copy$variantCapped") {
        dependsOn(copySoTask)
        dependsOn(copyDexTask)

        doLast {
            println("Dex and so files has been copied to ${rootProject.layout.projectDirectory.dir("out")}")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.core)
    implementation(projects.hiddenapi.bridge)
    implementation(projects.services.daemonService)
    implementation(projects.shared.android)
    implementation(projects.shared.java)
    implementation(phantom.kermit)
}