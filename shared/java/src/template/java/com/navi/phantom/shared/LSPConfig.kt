package com.navi.phantom.shared

object LSPConfig {
    val instance: LSPConfig = this

    var API_CODE: Int = ${apiCode}
    var VERSION_CODE: Int = ${verCode}
    var VERSION_NAME: String = "${verName}"
    var CORE_VERSION_CODE: Int = ${coreVerCode}
    var CORE_VERSION_NAME: String = "${coreVerName}"
}