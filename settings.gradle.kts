enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("core/gradle/libs.versions.toml"))
        }
        create("phantom") {
            from(files("gradle/phantom.versions.toml"))
        }
    }
}

rootProject.name = "PhantomLTU"

include(":app")
include(":apache")
include(":axml")
include(":core")
include(":hiddenapi:bridge")
include(":hiddenapi:stubs")
include(":services:daemon-service")
include(":services:manager-service")
include(":shared:java")
include(":shared:android")
include(":shared:java")
include(":shared:android")
include(":apkzlib")
include(":meta-loader")
include(":patch-loader")


project(":apache").projectDir = file("core/apache")
project(":axml").projectDir = file("core/axml")
project(":core").projectDir = file("core/core")
project(":hiddenapi").projectDir = file("core/hiddenapi")
project(":hiddenapi:bridge").projectDir = file("core/hiddenapi/bridge")
project(":hiddenapi:stubs").projectDir = file("core/hiddenapi/stubs")
project(":services").projectDir = file("core/services")
project(":services:daemon-service").projectDir = file("core/services/daemon-service")
project(":services:manager-service").projectDir = file("core/services/manager-service")