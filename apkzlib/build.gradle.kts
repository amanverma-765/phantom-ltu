val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

plugins {
    id("java-library")
    alias(phantom.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
}

dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    // Updated from jdk15on (deprecated) to jdk18on variant
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    api("com.google.guava:guava:33.5.0-jre")
    api("com.android.tools.build:apksig:8.13.2")
    compileOnlyApi("com.google.auto.value:auto-value-annotations:1.11.1")
    annotationProcessor("com.google.auto.value:auto-value:1.11.1")
}