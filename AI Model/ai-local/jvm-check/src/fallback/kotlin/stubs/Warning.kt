package com.secondlook

// Same two declarations as the keyboard's WarningBanner.kt (Android App), without the Android view code.
enum class WarningType {
    HIGH,
    MEDIUM
}

data class Warning(
    val type: WarningType,
    val headline: String,
    val tip: String
)
