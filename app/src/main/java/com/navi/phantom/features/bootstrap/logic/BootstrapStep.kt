package com.navi.phantom.features.bootstrap.logic

enum class BootstrapStep(val title: String, val description: String) {
    PARSE_APK("Parsing APK", "Reading manifest and extracting metadata"),
    SETUP_SIGNING("Setting Up Signing", "Configuring APK signature"),
    EXTRACT_SIGNATURE("Extracting Signature", "For signature bypass"),
    MODIFY_MANIFEST("Modifying Manifest", "Injecting component factory"),
    ADD_CONFIG("Adding Config", "Embedding bootstrap configuration"),
    ADD_METALOADER("Adding Metaloader", "Injecting metaloader.dex"),
    CREATE_LINKS("Creating Links", "Linking original APK entries"),
    WRITE_APK("Writing APK", "Finalizing bootstrapped APK"),
    COMPLETE("Complete", "Bootstrap finished successfully")
}

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

data class BootstrapStepState(
    val step: BootstrapStep,
    val status: StepStatus = StepStatus.PENDING,
    val details: List<String> = emptyList()
)
