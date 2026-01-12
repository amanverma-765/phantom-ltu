val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra


plugins {
    id("java-library")
    alias(phantom.plugins.kotlin.jvm)
    alias(phantom.plugins.serialization)
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
    sourceSets {
        main {
            java.srcDirs("libs/manifest-editor/lib/src/main/java")
            resources.srcDirs("libs/manifest-editor/lib/src/main")
        }
    }
}

dependencies {
    implementation(projects.axml)
    implementation(projects.apkzlib)
    implementation(projects.shared.java)
    implementation(phantom.commons.io)
    implementation(phantom.beust.jcommander)
    implementation(phantom.kermit)
    implementation(phantom.kotlinx.serialization.json)
}