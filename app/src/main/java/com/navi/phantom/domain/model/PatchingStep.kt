package com.navi.phantom.domain.model

enum class PatchingStep(val title: String, val description: String) {
    PARSE_APK("Parsing APK", "Reading manifest and extracting metadata"),
    SETUP_SIGNING("Setting Up Signing", "Configuring APK signature"),
    EXTRACT_SIGNATURE("Extracting Signature", "For signature bypass"),
    MODIFY_MANIFEST("Modifying Manifest", "Injecting component factory"),
    ADD_CONFIG("Adding Config", "Embedding patching configuration"),
    ADD_METALOADER("Adding Metaloader", "Injecting metaloader.dex"),
    CREATE_LINKS("Creating Links", "Linking original APK entries"),
    WRITE_APK("Writing APK", "Finalizing patched APK"),
    COMPLETE("Complete", "Patching finished successfully")
}
