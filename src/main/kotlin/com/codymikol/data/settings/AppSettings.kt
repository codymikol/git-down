package com.codymikol.data.settings

import com.fasterxml.jackson.annotation.JsonProperty

data class AppSettings(
    @JsonProperty("headerTextSize")
    val headerTextSize: Int = DEFAULT_HEADER_TEXT_SIZE,
    @JsonProperty("bodyTextSize")
    val bodyTextSize: Int = DEFAULT_BODY_TEXT_SIZE,
) {
    companion object {
        const val DEFAULT_HEADER_TEXT_SIZE = 20
        const val DEFAULT_BODY_TEXT_SIZE = 12
    }
}
