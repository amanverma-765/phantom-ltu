plugins {
    alias(libs.plugins.agp.app)
    alias(phantom.plugins.kotlin.android)
}

android {
    namespace = "com.navi.phantom.metaloader"

    defaultConfig {
        multiDexEnabled = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
        }
    }
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.replaceFirstChar { it.uppercase() }
    val variantLowered = variant.name.lowercase()

    val copyDexTask = tasks.register<Copy>("copyDex$variantCapped") {
        dependsOn("assemble$variantCapped")
        val dexDir = if (variant.buildType == "release") {
            layout.buildDirectory.dir("intermediates/dex/$variantLowered/minify${variantCapped}WithR8")
        } else {
            layout.buildDirectory.dir("intermediates/dex/$variantLowered/mergeDex$variantCapped")
        }
        from(dexDir)
        rename("classes.dex", "metaloader.dex")
        into(rootProject.layout.projectDirectory.dir("out/assets/${variant.name}/phantom"))
    }

    tasks.register("copy$variantCapped") {
        dependsOn(copyDexTask)

        doLast {
            println("Loader dex has been copied to ${rootProject.layout.projectDirectory.dir("out")}")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenapi.stubs)
    implementation(projects.shared.java)
    implementation(libs.hiddenapibypass)
    implementation(phantom.kermit)
}