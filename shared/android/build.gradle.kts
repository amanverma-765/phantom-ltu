plugins {
    alias(phantom.plugins.kotlin.android)
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "com.navi.phantom.shared"
    androidResources.enable = false
    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    implementation(projects.services.daemonService)
    implementation(phantom.kermit)
}